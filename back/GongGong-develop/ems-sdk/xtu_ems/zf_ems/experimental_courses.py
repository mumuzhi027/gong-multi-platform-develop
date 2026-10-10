"""实验课表与基础课表使用同一已登录会话，空列表表示没有实验课。"""
import re
from html import unescape

from xtu_ems.common.model import CourseInfo, CourseList
from xtu_ems.zf_ems.config import experimental_courses_url
from xtu_ems.common.exception import SessionInvalidException

DAY_NAMES = dict(zip("一二三四五六日天", ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "Sunday"]))
SCHEDULE = re.compile(r"星期([一二三四五六日天])\s*[\[【]\s*(\d+)(?:\s*[-–~至]\s*(\d+))?\s*节\s+([^\]】]+?)\s*[\]】]\s*(.*?)(?=\s*[,，;；]?\s*星期[一二三四五六日天]\s*[\[【]|$)", re.S)


def plain(value):
    return unescape(re.sub(r"<[^>]+>", " ", str(value or ""))).strip()


def parse_weeks(value):
    weeks = set()
    for part in re.split(r"[,，、;；]", value.replace("周", "")):
        numbers = [int(n) for n in re.findall(r"\d+", part)]
        if not numbers:
            continue
        candidates = range(numbers[0], numbers[1] + 1) if len(numbers) > 1 and re.search(r"[-–~至]", part) else numbers
        weeks.update(n for n in candidates if 1 <= n <= 60 and ("单" not in part or n % 2 == 1) and ("双" not in part or n % 2 == 0))
    return ",".join(map(str, sorted(weeks)))


def parse_experimental_courses(rows):
    courses = {}
    for row in rows:
        if not isinstance(row, dict):
            continue
        name = plain(row.get("kcmc"))
        if not name:
            continue
        for match in SCHEDULE.finditer(plain(row.get("sksjdd"))):
            day, first, last, weeks, classroom = match.groups()
            first, last = int(first), int(last or first)
            weeks = parse_weeks(weeks)
            if not (1 <= first <= last <= 12) or not weeks:
                continue
            course = CourseInfo(name=name, teacher=plain(row.get("jsxm")),
                classroom=classroom.strip(" ,，;；"), weeks=weeks,
                start_time=first, duration=last-first+1, day=DAY_NAMES[day],
                is_experiment=True, experiment_id=plain(row.get("xmz_id")),
                experiment_project=plain(row.get("xmmc")), experiment_group=plain(row.get("syfzmc")))
            key = (course.experiment_id, name, course.experiment_project, course.experiment_group, course.teacher, course.day, first, last, weeks)
            if key in courses:
                rooms = courses[key].classroom.split(" / ")
                if course.classroom and course.classroom not in rooms:
                    courses[key].classroom += " / " + course.classroom
            else:
                courses[key] = course
    return CourseList(courses=list(courses.values()))


async def get_experimental_courses(http_session, year, term):
    rows = []
    for page in range(1, 101):
        payload = {"xnm": year, "xqm": term, "kzlx": "ck", "xsdm": "", "kclbdm": "", "kclxdm": "",
            "queryModel.currentPage": page, "queryModel.showCount": 100,
            "queryModel.sortName": "", "queryModel.sortOrder": "asc"}
        async with http_session.post(experimental_courses_url, data=payload, timeout=15) as response:
            if response.status == 401:
                raise SessionInvalidException("Failed to fetch experimental courses")
            if response.status != 200:
                raise RuntimeError(f"Experimental course query returned HTTP {response.status}")
            result = await response.json(content_type=None)
        if isinstance(result, list):
            rows.extend(result)
            break
        if not isinstance(result, dict):
            raise ValueError("Unexpected experimental course response")
        items = result.get("items", result.get("rows", []))
        if not isinstance(items, list):
            raise ValueError("Unexpected experimental course rows")
        rows.extend(items)
        total = int(result.get("totalPage", result.get("total", 1)) or 1)
        if not items or page >= total:
            break
    else:
        raise ValueError("Experimental course pagination limit exceeded")
    return parse_experimental_courses(rows)
