import asyncio
import logging
import re
import time
from datetime import datetime
from io import BytesIO
from typing import Literal

import bs4
import pdfplumber

from xtu_ems.common.exception import ServiceUnavailableException, SessionInvalidException
from xtu_ems.common.model import RankInfo, Score, ScoreBoard
from xtu_ems.common.sess import HttpSessionHolder

logger = logging.getLogger(__name__)

TRANSCRIPT_PREPARE_URL = "https://jw.xtu.edu.cn/jwglxt/bysxxcx/xscjzbdy_cxXsCount.html?gnmkdm=N558020"
TRANSCRIPT_EXPORT_URL = "https://jw.xtu.edu.cn/jwglxt/bysxxcx/xscjzbdy_dyList.html?gnmkdm=N558020"
RANK_PAGE_URL = "https://jw.xtu.edu.cn/jwglxt/cjpmtj/cjpmtj_cxPjxfjdpmtjIndex.html?gnmkdm=N309104&layout=default"
RANK_QUERY_URL = "https://jw.xtu.edu.cn/jwglxt/cjpmtj/cjpmtj_cxPjxfjdpmtjIndex.html?doType=query&gnmkdm=N309104"
WEIGHTED_AVERAGE_RANK_URL = "https://jw.xtu.edu.cn/jwglxt/tmgl4/tmsq_cxTmsqIndex.html?gnmkdm=N104906&layout=default"
RANK_QUERY_TIMEOUT_SECONDS = 60

COLLEGE_LABELS = ("学院:", "\u701b\ufe42\u6acc:")
MAJOR_LABELS = ("专业:", "\u6d93\u64b2\u7b1f:")
STUDENT_ID_LABELS = ("学号:", "\u701b\ufe40\u5f7f:")
NAME_LABELS = ("姓名:", "\u6fee\u64b3\u6095:")

HEADER_NAME_ALIASES = {"课程名称", "\u7487\u5267\u25bc\u935a\u5d87\u041e"}
HEADER_TYPE_ALIASES = {"课程性质", "\u7487\u5267\u25bc\u93ac\u0446\u5ddd"}
HEADER_CREDIT_ALIASES = {"学分", "\u701b\ufe40\u578e"}
HEADER_SCORE_ALIASES = {"成绩", "\u93b4\u612e\u54d7"}
TERM_MARKERS = {"学期", "\u701b\ufe3d\u6e61"}
CET_EXAM_NAMES = {"CET4", "CET6"}
CET_TEXT_PATTERN = re.compile(r"(CET[-\s]?[46]).{0,20}?(\d{3})", re.IGNORECASE)

COURSE_TYPE_MAP = {
    "必修": "必修",
    "选修": "选修",
    "跨学科选修": "跨学科选修",
    "\u8e47\u546c\u6168": "必修",
    "\u95ab\u5909\u6168": "选修",
    "\u74ba\u3125\ue11f\u7ec9\u6226\u20ac\u5909\u6168": "跨学科选修",
}


def with_default(value, default):
    return value if value is not None and value.strip() != "" else default


def normalize_cell(value: str | None) -> str:
    return "".join((value or "").split())


def extract_between(text: str, start_labels: tuple[str, ...], end_labels: tuple[str, ...] | None = None) -> str:
    for start_label in start_labels:
        if start_label not in text:
            continue
        remaining = text.split(start_label, 1)[1]
        if not end_labels:
            return remaining.strip()
        for end_label in end_labels:
            if end_label in remaining:
                return remaining.split(end_label, 1)[0].strip()
    return ""


def find_column_index(header_row: list[str | None], start: int, end: int, aliases: set[str]) -> int | None:
    for idx in range(start, end):
        if normalize_cell(header_row[idx]) in aliases:
            return idx
    return None


def normalize_course_type(value: str) -> Literal["必修", "选修", "跨学科选修"]:
    return COURSE_TYPE_MAP.get(normalize_cell(value), "跨学科选修")


def normalize_score(value: str) -> str:
    return value.replace("△", "").replace("*", "").strip()


def normalize_exam_name(value: str) -> str:
    return normalize_cell(value).upper().replace("-", "")


def extract_numeric_score(value: str | None) -> str:
    match = re.search(r"\d+", value or "")
    return match.group(0) if match else ""


def set_cet_score(scoreboard: ScoreBoard, exam_name: str, score: str) -> None:
    if not score:
        return
    if exam_name == "CET4":
        scoreboard.cet4 = score
    elif exam_name == "CET6":
        scoreboard.cet6 = score


def extract_cet_scores_from_text(scoreboard: ScoreBoard, text: str) -> None:
    for exam_name, score in CET_TEXT_PATTERN.findall(text):
        set_cet_score(scoreboard, normalize_exam_name(exam_name), score)


def safe_table_value(table: list[list[str | None]], row_idx: int, col_idx: int) -> str | None:
    try:
        row = table[row_idx]
        if row is None:
            return None
        return row[col_idx]
    except IndexError:
        return None


def parse_transcript(transcript_pdf: pdfplumber.PDF) -> ScoreBoard:
    page = transcript_pdf.pages[0]
    text_lines = page.extract_text_lines() or []
    page_text = page.extract_text() or ""
    if len(text_lines) < 2:
        raise ServiceUnavailableException("ZF EMS Transcript Service", "Transcript PDF format changed")

    text = text_lines[1]["text"]

    scoreboard = ScoreBoard()
    scoreboard.college = extract_between(text, COLLEGE_LABELS, MAJOR_LABELS)
    scoreboard.major = extract_between(text, MAJOR_LABELS, STUDENT_ID_LABELS)
    scoreboard.student_id = extract_between(text, STUDENT_ID_LABELS, NAME_LABELS)
    scoreboard.name = extract_between(text, NAME_LABELS)

    table = page.extract_table()
    if not table or len(table) < 4 or not table[0]:
        raise ServiceUnavailableException("ZF EMS Transcript Service", "Transcript table is missing")

    term = 0
    header_row = table[0]
    group_count = (len(header_row) + 6) // 7

    for col in range(group_count):
        start = col * 7
        end = min(len(header_row), (col + 1) * 7)
        name_idx = find_column_index(header_row, start, end, HEADER_NAME_ALIASES)
        type_idx = find_column_index(header_row, start, end, HEADER_TYPE_ALIASES)
        credit_idx = find_column_index(header_row, start, end, HEADER_CREDIT_ALIASES)
        score_idx = find_column_index(header_row, start, end, HEADER_SCORE_ALIASES)

        if None in (name_idx, type_idx, credit_idx, score_idx):
            continue

        for row in table[1:-3]:
            if not row or not row[name_idx]:
                break

            row_name = row[name_idx].strip()
            normalized_name = normalize_cell(row_name)
            if any(marker in normalized_name for marker in TERM_MARKERS):
                term += 1
                continue

            exam_name = normalize_exam_name(row_name)
            if exam_name in CET_EXAM_NAMES:
                set_cet_score(scoreboard, exam_name, extract_numeric_score(row[score_idx]))
                continue

            if row[type_idx] and row[credit_idx] and row[score_idx]:
                scoreboard.scores.append(
                    Score(
                        name=row_name,
                        type=normalize_course_type(row[type_idx]),
                        credit=row[credit_idx],
                        score=normalize_score(row[score_idx]),
                        term=term,
                    )
                )

    scoreboard.average_score = with_default(safe_table_value(table, -3, 2), "100")
    scoreboard.gpa = with_default(safe_table_value(table, -3, 16), "4.0")
    scoreboard.total_credit = (
        with_default(safe_table_value(table, -1, 0), "0"),
        with_default(safe_table_value(table, -1, 1), "0"),
    )
    scoreboard.compulsory_credit = (
        with_default(safe_table_value(table, -1, 4), "0"),
        with_default(safe_table_value(table, -1, 8), "0"),
    )
    scoreboard.elective_credit = (
        with_default(safe_table_value(table, -1, 9), "0"),
        with_default(safe_table_value(table, -1, 12), "0"),
    )
    scoreboard.cross_course_credit = (
        with_default(safe_table_value(table, -1, 15), "0"),
        with_default(safe_table_value(table, -1, 17), "0"),
    )
    if not scoreboard.cet4 or not scoreboard.cet6:
        extract_cet_scores_from_text(scoreboard, page_text)
    return scoreboard


async def get_transcript_scoreboard(session: HttpSessionHolder) -> ScoreBoard:
    payload = (
        "gsdygx=10530-zw-qcmrgs"
        "&ids="
        "&bdykcxzDms="
        "&cytjkcxzDms="
        "&cytjkclbDms="
        "&cytjkcgsDms="
        "&bjgbdykcxzDms="
        "&bjgbdyxxkcxzDms="
        "&djksxmDms="
        "&cjbzmcDms="
        "&zdyfsxmDms="
        "&bdymaxcjbzmcDms="
        "&cjdySzxs="
    )
    async with session.to_aiohttp_session() as http_session:
        async with http_session.post(
            url=TRANSCRIPT_PREPARE_URL,
            allow_redirects=False,
        ) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to fetch transcript page")
            logger.debug(await response.text())

        async with http_session.post(
            url=TRANSCRIPT_EXPORT_URL,
            data=payload,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
            allow_redirects=False,
        ) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to fetch transcript PDF")
            resource_url = await response.text()

        pdf_url = "https://jw.xtu.edu.cn" + resource_url.replace('"', "").replace("\\", "")
        logger.info("get pdf from [%s]", pdf_url)
        async with http_session.get(
            url=pdf_url,
            allow_redirects=False,
        ) as response:
            if response.status != 200:
                raise ServiceUnavailableException("ZF EMS Transcript Service", "Failed to download transcript PDF")
            pdf_bytes = await response.read()

    with pdfplumber.open(BytesIO(pdf_bytes)) as pdf:
        return parse_transcript(pdf)


def build_rank_query_payload(
    start_term: str = "202012",
    end_term: str = "",
    course_type: str = "",
    time_marker: str = "1",
) -> dict[str, str]:
    return {
        "qsXnxq": start_term,
        "zzXnxq": end_term,
        "xbx": course_type,
        "_search": "false",
        "nd": str(int(time.time() * 1000)),
        "queryModel.showCount": "50",
        "queryModel.currentPage": "1",
        "queryModel.sortName": "xh ",
        "queryModel.sortOrder": "asc",
        "time": time_marker,
    }


def parse_rank_payload(payload: dict) -> RankInfo:
    items = payload.get("items") or []
    if not items:
        raise ServiceUnavailableException("ZF EMS Rank Service", "No ranking data returned")

    item = items[0]
    return RankInfo(
        average_score=str(item.get("pjcj", "0")),
        gpa=str(item.get("pjxfjd", "0")),
        class_rank=int(item.get("jdbjpm", 0) or 0),
        major_rank=int(item.get("jdnjzypm", 0) or 0),
        terms=[
            str(item.get("qsxnxq", "")),
            str(item.get("zzxnxq", "")),
        ],
    )


def parse_weighted_average_rank(page_html: str) -> int:
    soup = bs4.BeautifulSoup(page_html, "html.parser")
    for label in soup.find_all("label"):
        if "加权平均成绩排名" not in label.get_text(strip=True):
            continue

        container = label.parent
        value = container.find("span", class_="form-control-static") if container else None
        if value is None:
            value = label.find_next("span", class_="form-control-static")

        match = re.search(r"\d+", value.get_text(strip=True) if value else "")
        if match:
            return int(match.group(0))

    raise ServiceUnavailableException(
        "ZF EMS Weighted Average Rank Service",
        "Weighted average rank is missing from page",
    )


async def weighted_average_rank_getter(session: HttpSessionHolder) -> int:
    async with session.to_aiohttp_session() as http_session:
        async with http_session.get(
            url=WEIGHTED_AVERAGE_RANK_URL,
            allow_redirects=False,
        ) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to open weighted average rank page")
            return parse_weighted_average_rank(await response.text())


async def rank_getter_with_options(
    session: HttpSessionHolder,
    *,
    start_term: str = "202012",
    end_term: str = "",
    course_type: str = "",
    time_marker: str = "1",
) -> RankInfo:
    payload = build_rank_query_payload(
        start_term=start_term,
        end_term=end_term,
        course_type=course_type,
        time_marker=time_marker,
    )

    async with session.to_aiohttp_session() as http_session:
        async with http_session.get(
            url=RANK_PAGE_URL,
            allow_redirects=False,
        ) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to open rank page")
            await response.text()

        try:
            async with http_session.post(
                url=RANK_QUERY_URL,
                data=payload,
                headers={
                    "Accept": "application/json, text/javascript, */*; q=0.01",
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                    "X-Requested-With": "XMLHttpRequest",
                },
                allow_redirects=False,
                timeout=RANK_QUERY_TIMEOUT_SECONDS,
            ) as response:
                if response.status != 200:
                    raise SessionInvalidException("Failed to fetch rank data")
                rank_payload = await response.json(content_type=None)
        except asyncio.TimeoutError as exc:
            raise ServiceUnavailableException("ZF EMS Rank Service", "Rank query timeout") from exc

    return parse_rank_payload(rank_payload)


async def rank_getter(session: HttpSessionHolder) -> RankInfo:
    return await rank_getter_with_options(session)


async def compulsory_rank_getter(session: HttpSessionHolder) -> RankInfo:
    rank = await rank_getter_with_options(
        session,
        start_term="202012",
        end_term="",
        course_type="bx",
        time_marker="0",
    )
    rank.weighted_average_rank = await weighted_average_rank_getter(session)
    return rank


async def empty_transcript_scoreboard(session: HttpSessionHolder) -> ScoreBoard:
    return ScoreBoard()


async def empty_rank_getter(session: HttpSessionHolder):
    return RankInfo(average_score="0", gpa="0", class_rank=0, major_rank=0, terms=[])


if __name__ == "__main__":
    file = "/home/leo/Project/Project/Python/GongGong/score1.pdf"
    with pdfplumber.open(file) as pdf:
        scoreboard = parse_transcript(pdf)
        print(scoreboard.model_dump_json(indent=4))
