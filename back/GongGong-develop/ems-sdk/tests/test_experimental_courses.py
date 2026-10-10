import asyncio
import json
from pathlib import Path

import pytest
from xtu_ems.common.exception import SessionInvalidException
from xtu_ems.zf_ems.experimental_courses import get_experimental_courses, parse_experimental_courses


def row(schedule="星期二 [7-8节 10周] 工科楼南楼607", **changes):
    return {"kcmc": "数据结构与算法", "xmmc": "二叉树寻找", "xmz_id": "lab-1",
        "jsxm": "老师", "syfzmc": "分组2", "sksjdd": schedule, **changes}


def test_schedule_and_duplicate_rooms():
    courses = parse_experimental_courses([row("星期二 [7-8节 10周] 工科楼南楼607 ,星期二 [7-8节 10周] 工科楼南楼612"), row("星期二 [7-8节 10周] 工科楼南楼607")]).courses
    assert len(courses) == 1
    course = courses[0]
    assert (course.name, course.day, course.weeks, course.start_time, course.duration) == ("数据结构与算法", "Tuesday", "10", 7, 2)
    assert course.classroom == "工科楼南楼607 / 工科楼南楼612"
    assert course.is_experiment and course.experiment_project == "二叉树寻找"
    assert course.experiment_group == "分组2"


def test_multiple_occurrences_and_week_ranges():
    courses = parse_experimental_courses([row("星期一 [1-4节 9-13周(单)] 教室A,星期五 [5-6节 10,12周] 教室B")]).courses
    assert [(c.day, c.weeks, c.start_time, c.duration) for c in courses] == [("Monday", "9,11,13", 1, 4), ("Friday", "10,12", 5, 2)]


def test_missing_rows_and_bad_schedules_do_not_create_courses():
    assert not parse_experimental_courses([]).courses
    assert not parse_experimental_courses([{}, None, row("时间地点待定"), row("星期二 [13-14节 10周] A"), row("星期二 [8-7节 10周] A")]).courses


def test_keeps_twelfth_period_and_project_identity():
    courses = parse_experimental_courses([row("星期一 [9-12节 13周] 工训中心308"), row("星期一 [9-12节 13周] 工训中心308", xmmc="第二个项目", xmz_id="lab-2")]).courses
    assert len(courses) == 2
    assert all(c.duration == 4 and c.start_time == 9 for c in courses)


class Response:
    def __init__(self, payload, status=200):
        self.payload, self.status = payload, status

    async def __aenter__(self):
        return self

    async def __aexit__(self, *args):
        pass

    async def json(self, **kwargs):
        return self.payload


class Session:
    def __init__(self, responses):
        self.responses, self.requests = list(responses), []

    def post(self, url, **kwargs):
        self.requests.append((url, kwargs))
        return self.responses.pop(0)

    async def __aenter__(self):
        return self

    async def __aexit__(self, *args):
        pass


@pytest.mark.parametrize("lab_response, expected_count", [(Response({"items": [row()]}), 2), (Response({"items": []}), 1), (Response({}, 404), 1)])
def test_combines_basic_and_optional_labs(lab_response, expected_count):
    from xtu_ems.zf_ems.courses import get_courses
    session = Session([Response({"kbList": [{"kcmc": "基础课程", "jc": "7-8节", "zcd": "1-20周", "xqj": "2"}]}), lab_response])

    class Holder:
        metadata = {}

        def to_aiohttp_session(self):
            return session

    courses = asyncio.run(get_courses(Holder(), year=2026, term=1)).courses
    assert len(courses) == expected_count
    assert courses[0].name == "基础课程" and not courses[0].is_experiment


def test_fetches_all_pages_in_same_term():
    session = Session([Response({"items": [row()], "totalPage": 2}), Response({"items": [row("星期五 [5-6节 16周] B", xmz_id="lab-2")], "totalPage": 2})])
    courses = asyncio.run(get_experimental_courses(session, 2026, 3)).courses
    assert len(courses) == 2
    assert [r[1]["data"]["queryModel.currentPage"] for r in session.requests] == [1, 2]
    assert all(r[1]["data"]["xqm"] == 3 and r[1]["data"]["xnm"] == 2026 for r in session.requests)
    assert "/xssygl/sykbcx_cxSykbcxxsIndex.html" in session.requests[0][0]


def test_no_experiment_and_expired_session():
    assert not asyncio.run(get_experimental_courses(Session([Response({"items": [], "totalPage": 0})]), 2026, 3)).courses
    with pytest.raises(SessionInvalidException):
        asyncio.run(get_experimental_courses(Session([Response({}, 401)]), 2026, 3))


def test_user_portal_fixture():
    rows = json.loads((Path(__file__).parent / "fixtures" / "experimental-courses.json").read_text(encoding="utf-8"))
    courses = parse_experimental_courses(rows).courses
    assert len(courses) == 15
    assert sum(c.start_time + c.duration - 1 == 12 for c in courses) == 5
    assert all(c.weeks and c.is_experiment for c in courses)
