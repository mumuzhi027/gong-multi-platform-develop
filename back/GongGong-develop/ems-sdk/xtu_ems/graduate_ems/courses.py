import datetime
import html
import logging
import re
from urllib.parse import urljoin, urlparse

import bs4

from xtu_ems.common.exception import *
from xtu_ems.common.model import CourseList, CourseInfo
from xtu_ems.common.sess import HttpSessionHolder
from xtu_ems.graduate_ems.config import get_term_code

logger = logging.getLogger(__name__)
GMS_INDEX_URL = "https://gmsstu.xtu.edu.cn/index"


def _split_courses(value: str | None) -> list[str]:
    if not value:
        return []
    normalized = html.unescape(value).replace("##", "")
    # 页面脚本会把接口原始数据中的 [周次]/[教室] 转成 span；两种输入均兼容。
    normalized = re.sub(
        r"<span[^>]*>(.*?)</span>",
        r"[\1]",
        normalized,
        flags=re.IGNORECASE | re.DOTALL,
    )
    return [
        re.sub(r"<[^>]+>", "", item).strip()
        for item in re.split(r"<br\s*/?>", normalized, flags=re.IGNORECASE)
        if re.sub(r"<[^>]+>", "", item).strip()
    ]


def _parse_course_description(course: str) -> tuple[str, str, str, str]:
    """Parse `课程(班级)[周次]教师[教室]` returned by the graduate system."""
    bracket_matches = list(re.finditer(r"[\[【]([^\]】]*)[\]】]", course))
    if len(bracket_matches) < 2:
        raise ValueError(f"Unsupported graduate course description: {course}")

    first, last = bracket_matches[0], bracket_matches[-1]
    course_with_class = course[:first.start()].strip()
    # 只移除末尾的“...班”，避免误删课程名称本身自带的括号。
    course_name = re.sub(r"[（(][^（）()]*班[）)]\s*$", "", course_with_class).strip()
    weeks = first.group(1).replace("周", "").strip()
    teacher = course[first.end():last.start()].strip()
    classroom = last.group(1).strip()
    return course_name or course_with_class, weeks, teacher, classroom


def _extract_load_url(page_html: str, page_url: str) -> str | None:
    ajax_blocks = re.split(r"\$\s*\.\s*ajax\s*\(", page_html, flags=re.IGNORECASE)
    for block in ajax_blocks[1:]:
        block = block[:5000]
        if not re.search(r"type\s*:\s*['\"]post['\"]", block, flags=re.IGNORECASE):
            continue
        if not re.search(r"\bkblx\b", block) or not re.search(r"\btermcode\b", block):
            continue
        match = re.search(r"url\s*:\s*['\"]([^'\"]+)['\"]", block, flags=re.IGNORECASE)
        if match:
            return urljoin(page_url, match.group(1))
    return None


def _find_inner_course_pages(page_html: str, page_url: str) -> list[str]:
    """Find the “我的课表” inner page hidden behind the graduate `/tab` wrapper."""
    soup = bs4.BeautifulSoup(page_html, "html.parser")
    preferred: list[str] = []
    fallback: list[str] = []

    def add(raw_url: str | None, target: list[str]):
        if not raw_url or raw_url.startswith(("#", "javascript:")):
            return
        absolute = urljoin(page_url, raw_url)
        parsed = urlparse(absolute)
        if parsed.hostname != "gmsstu.xtu.edu.cn" or "logout" in parsed.path.lower():
            return
        if absolute not in target:
            target.append(absolute)

    for element in soup.find_all(True):
        marker = " ".join(
            filter(None, [element.get("title", ""), element.get_text(" ", strip=True)])
        )
        target = preferred if "我的课表" in marker else fallback
        if "我的课表" in marker or element.name == "iframe":
            for attribute in ("href", "src", "data-url", "data-href"):
                add(element.get(attribute), target)

    # EasyUI 的 tab 经常只在 JavaScript 配置中保存 href/url。
    for block in re.split(r"\$\s*\.\s*ajax\s*\(", page_html, flags=re.IGNORECASE)[1:]:
        match = re.search(r"url\s*:\s*['\"]([^'\"]+)['\"]", block[:3000], flags=re.IGNORECASE)
        if match:
            add(match.group(1), preferred if "我的课表" in block[:3000] else fallback)

    for marker in re.finditer("我的课表", page_html):
        nearby = page_html[max(0, marker.start() - 800):marker.end() + 800]
        for match in re.finditer(
            r"(?:href|src|url)\s*[:=]\s*['\"]([^'\"]+)['\"]",
            nearby,
            flags=re.IGNORECASE,
        ):
            add(match.group(1), preferred)
        # 兼容 onclick="addTab('我的课表', '/混淆地址')" 一类写法。
        for match in re.finditer(r"['\"](/[^'\"]+)['\"]", nearby):
            add(match.group(1), preferred)

    return preferred + fallback


def _extract_current_week(page_html: str) -> int | None:
    text = bs4.BeautifulSoup(page_html, "html.parser").get_text(" ", strip=True)
    match = re.search(r"当前为?第\s*(\d+)\s*周", text)
    if not match:
        match = re.search(r"当前周(?:次)?\s*[：:]?\s*(\d+)", text)
    return int(match.group(1)) if match else None


def parse_course_time(rows: list[dict]) -> CourseList:
    """
    解析课程时间

    :param rows: 每一行的课程
    :return: 解析后的课程列表
    """
    time_order = {
        "上午1": 1,
        "上午2": 2,
        "上午3": 3,
        "上午4": 4,
        "下午1": 5,
        "下午2": 6,
        "下午3": 7,
        "下午4": 8,
        "晚上1": 9,
        "晚上2": 10,
        "晚上3": 11,
        "无节次": 12
    }

    # 接口偶尔会改变行顺序或增加未知行，先过滤再排序，避免整个课表解析失败。
    ordered_rows = sorted(
        (row for row in rows if row.get("mc") in time_order),
        key=lambda row: time_order[row["mc"]]
    )
    parsed_courses = []
    visited: set[tuple[int, str, str]] = set()
    for idx, z in enumerate(["z1", "z2", "z3", "z4", "z5", "z6", "z7"]):  # idx: 1 ... 7; z: z1 ... z7
        for i, row in enumerate(ordered_rows):
            if row["mc"] == "无节次":
                continue
            for course in _split_courses(row.get(z)):
                key = (i, z, course)
                if key in visited:
                    continue

                section = 1
                visited.add(key)
                for j in range(i + 1, len(ordered_rows)):
                    if course not in _split_courses(ordered_rows[j].get(z)):
                        break
                    visited.add((j, z, course))
                    section += 1

                try:
                    name, weeks, teacher, classroom = _parse_course_description(course)
                except ValueError:
                    logger.warning("忽略无法解析的研究生课程：%s", course, exc_info=True)
                    continue
                parsed_courses.append(CourseInfo(
                    name=name,
                    weeks=weeks,
                    day=["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"][idx],  # 星期几
                    start_time=time_order[row["mc"]],
                    duration=section,  # 持续节次
                    teacher=teacher,
                    classroom=classroom
                ))
    return CourseList(courses=parsed_courses)


async def get_course_context(session: HttpSessionHolder) -> tuple[str, int | None]:
    """
    获取课程链接

    :return: 课程链接
    """
    async with session.to_aiohttp_session() as http_session:
        async with http_session.get(GMS_INDEX_URL) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to access index page")
            text = await response.text()

            soup = bs4.BeautifulSoup(text, "html.parser")
            link = soup.find("a", attrs={"title": "学生课表查询"})
            if not link or not link.get("href"):
                raise ServiceUnavailableException("Course link not found")
            page_url = urljoin(GMS_INDEX_URL, link["href"])
        pending = [page_url]
        visited: set[str] = set()
        diagnostics = []
        while pending and len(visited) < 8:
            current_url = pending.pop(0)
            if current_url in visited:
                continue
            visited.add(current_url)
            async with http_session.get(current_url) as response:
                if response.status != 200:
                    continue
                text = await response.text()
                final_page_url = str(response.url)
                page_soup = bs4.BeautifulSoup(text, "html.parser")
                page_title = page_soup.title.get_text(" ", strip=True) if page_soup.title else ""
                diagnostics.append(
                    (final_page_url, page_title, len(text), "ajax" in text.lower(), "kblx" in text)
                )

                load_url = _extract_load_url(text, final_page_url)
                if load_url:
                    return load_url, _extract_current_week(text)
                for inner_url in _find_inner_course_pages(text, final_page_url):
                    if inner_url not in visited and inner_url not in pending:
                        pending.append(inner_url)

        logger.error("研究生课表加载接口未找到：visited=%s", diagnostics)
        raise ServiceUnavailableException("Course form action not found")


async def get_course_link(session: HttpSessionHolder) -> str:
    return (await get_course_context(session))[0]


async def get_current_week(session: HttpSessionHolder) -> int:
    _, current_week = await get_course_context(session)
    if current_week is None or current_week < 1:
        raise ServiceUnavailableException("Current graduate teaching week not found")
    return current_week


async def get_courses(session: HttpSessionHolder) -> CourseList:
    """
    获取课程列表

    :param session: 已登录的 HttpSessionHolder 对象
    :return: 课程列表，每个课程是一个字典
    """
    async with session.to_aiohttp_session() as http_session:
        term_code = get_term_code(datetime.datetime.now().date())
        payload = {
            "kblx": "xs",
            "termcode": term_code
        }
        url = await get_course_link(session)
        async with http_session.post(
            url=url,
            data=payload,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        ) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to fetch courses")
            result = await response.json(content_type=None)
            rows = result.get("rows") or []
            courses = parse_course_time(rows)
            logger.info(
                "研究生课表解析完成：termcode=%s, rows=%s, courses=%s",
                term_code,
                len(rows),
                len(courses.courses),
            )
            return courses
