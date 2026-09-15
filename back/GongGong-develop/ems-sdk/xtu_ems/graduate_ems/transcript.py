import json
import logging
import re
from urllib.parse import urljoin

from bs4 import BeautifulSoup

from xtu_ems.common.exception import ServiceUnavailableException, SessionInvalidException
from xtu_ems.common.model import RankInfo, Score, ScoreBoard
from xtu_ems.common.sess import HttpSessionHolder
from xtu_ems.graduate_ems.personal_info import (
    GMS_INDEX_URL,
    _decode_obfuscated_route,
    _find_personal_page,
    get_student_info,
)

logger = logging.getLogger(__name__)

SCORE_FIELD_ALIASES = {
    "name": ("kcmc", "kcname", "course_name", "coursename", "kc_mc", "课程名称", "课程"),
    "score": ("cj", "zcj", "kscj", "score", "cjxs", "grade", "成绩", "总成绩"),
    "credit": ("xf", "kcxf", "credit", "credits", "xuefen", "学分"),
    "type": ("xwk", "kcxzmc", "kcxz", "kclbmc", "kclb", "course_type", "kclx", "课程性质", "课程类别"),
    "term": ("xxxq", "termcode", "xnxq", "xnxqmc", "xq", "term", "semester", "学期"),
}

SUMMARY_ALIASES = {
    "degree_credit": ("xwxf",),
    "required_degree_credit": ("yxxwxf", "faxwxf"),
    "non_degree_credit": ("fxwxf",),
    "required_non_degree_credit": ("yxfxwxf",),
    "total_credit": ("zxf",),
    "required_total_credit": ("yxzxf", "fazxf"),
    "gpa": ("pjjd",),
    "score_rank": ("xsgpapm",),
    "total_people": ("zrs",),
    "total_score_rank": ("xszcjpm",),
    "degree_course_rank": ("xwkpm",),
    "gpa_rank": ("gpapm",),
}

SCORE_PAGE_TITLES = ("课程成绩查询", "成绩查询", "我的成绩")


def _clean(value) -> str:
    if value is None or isinstance(value, (dict, list)):
        return ""
    return re.sub(r"\s+", " ", str(value)).strip()


def _value(row: dict, aliases: tuple[str, ...]) -> str:
    normalized = {str(key).lower(): value for key, value in row.items()}
    for alias in aliases:
        value = _clean(normalized.get(alias.lower()))
        if value:
            return value
    return ""


def _iter_lists(value):
    if isinstance(value, list):
        yield value
        for child in value:
            yield from _iter_lists(child)
    elif isinstance(value, dict):
        for child in value.values():
            yield from _iter_lists(child)


def _iter_lists_with_path(value, path: tuple[str, ...] = ()):
    if isinstance(value, list):
        yield path, value
        for index, child in enumerate(value):
            yield from _iter_lists_with_path(child, path + (str(index),))
    elif isinstance(value, dict):
        for key, child in value.items():
            yield from _iter_lists_with_path(child, path + (str(key),))


def _find_score_page(index_html: str) -> str | None:
    soup = BeautifulSoup(index_html, "html.parser")
    for element in soup.find_all(True):
        marker = " ".join(filter(None, [element.get("title", ""), element.get_text(" ", strip=True)]))
        if not any(title in marker for title in SCORE_PAGE_TITLES):
            continue
        for attribute in ("href", "src", "data-url", "data-href"):
            raw_url = element.get(attribute)
            if raw_url and not raw_url.startswith(("#", "javascript:")):
                return urljoin(GMS_INDEX_URL, raw_url)

    for title in SCORE_PAGE_TITLES:
        for marker in re.finditer(title, index_html):
            nearby = index_html[max(0, marker.start() - 800):marker.end() + 800]
            for match in re.finditer(
                r"(?:href|src|url)\s*[:=]\s*['\"]([^'\"]+)['\"]",
                nearby,
                flags=re.IGNORECASE,
            ):
                raw_url = match.group(1)
                if not raw_url.startswith(("#", "javascript:")):
                    return urljoin(GMS_INDEX_URL, raw_url)
            for match in re.finditer(r"['\"](/[^'\"]+)['\"]", nearby):
                return urljoin(GMS_INDEX_URL, match.group(1))
    return None


def _find_score_rows(payload) -> list[tuple[dict, str]]:
    result: list[tuple[dict, str]] = []
    seen = set()
    for path, candidate in _iter_lists_with_path(payload):
        rows = [row for row in candidate if isinstance(row, dict)]
        if not rows:
            continue
        score = sum(
            bool(_value(row, SCORE_FIELD_ALIASES["name"]))
            + bool(_value(row, SCORE_FIELD_ALIASES["score"]))
            + bool(_value(row, SCORE_FIELD_ALIASES["credit"]))
            for row in rows
        )
        if score < max(2, len(rows)):
            continue
        path_parts = {part.lower() for part in path}
        path_text = "/".join(path).lower()
        hinted_type = ""
        if (
            "非学位" in path_text
            or "fxw" in path_text
            or "nondegree" in path_text
            or path_parts.intersection({"xx", "xxlist", "elective", "optional"})
        ):
            hinted_type = "选修"
        elif (
            "学位" in path_text
            or "xwk" in path_text
            or "degree" in path_text
            or path_parts.intersection({"bx", "bxlist", "compulsory", "required"})
        ):
            hinted_type = "必修"
        for row in rows:
            identity = tuple(sorted((str(key), _clean(value)) for key, value in row.items()))
            if identity in seen:
                continue
            seen.add(identity)
            result.append((row, hinted_type))
    return result


def _course_type(raw: str) -> str:
    normalized = raw.replace(" ", "")
    if (
        "非学位" in normalized
        or "选修" in normalized
        or normalized.lower() in ("0", "否", "n", "false", "fxw", "nondegree", "xx")
    ):
        return "选修"
    return "必修"


def _term_ordinal(raw: str, account: str) -> int:
    admission_match = re.match(r"(20\d{2})", account)
    admission_year = int(admission_match.group(1)) if admission_match else None

    raw = raw.strip()
    if raw.isdigit():
        number = int(raw)
        # 研究生系统 termcode：2025-2026-1 对应 51，每学期递增 1。
        if number >= 40 and admission_year:
            admission_code = 51 + (admission_year - 2025) * 2
            return max(1, number - admission_code + 1)
        if 1 <= number <= 20:
            return number

    year_match = re.search(r"(20\d{2})\D+(20\d{2})", raw)
    term_match = re.search(r"(?:第|[-_/])\s*([12一二])\s*(?:学期)?", raw)
    if year_match and admission_year:
        term_value = term_match.group(1) if term_match else "1"
        term_no = 2 if term_value in ("2", "二") else 1
        return max(1, (int(year_match.group(1)) - admission_year) * 2 + term_no)
    return 1


def _float(value: str) -> float:
    match = re.search(r"-?\d+(?:\.\d+)?", value)
    return float(match.group(0)) if match else 0.0


def _display_number(value: float) -> str:
    return str(int(value)) if value.is_integer() else f"{value:.2f}".rstrip("0").rstrip(".")


def _payload_value(payload, aliases: tuple[str, ...]) -> str:
    alias_set = {alias.lower() for alias in aliases}
    if isinstance(payload, dict):
        for key, value in payload.items():
            if str(key).lower() in alias_set:
                cleaned = _clean(value)
                if cleaned:
                    return cleaned
            nested = _payload_value(value, aliases)
            if nested:
                return nested
    elif isinstance(payload, list):
        for value in payload:
            nested = _payload_value(value, aliases)
            if nested:
                return nested
    return ""


def _html_value(page_html: str, aliases: tuple[str, ...]) -> str:
    soup = BeautifulSoup(page_html, "html.parser")
    for element_id in aliases:
        element = soup.find(id=element_id)
        if element:
            value = _clean(element.get_text(" ", strip=True))
            if value:
                return value
    return ""


def _summary_value(payload, page_html: str, name: str) -> str:
    aliases = SUMMARY_ALIASES[name]
    return _payload_value(payload, aliases) or _html_value(page_html, aliases)


def _summary_number(payload, page_html: str, name: str) -> float:
    return _float(_summary_value(payload, page_html, name))


def parse_score_payload(payload, account: str, page_html: str = "") -> ScoreBoard:
    rows = _find_score_rows(payload)
    scores = []
    weighted_total = 0.0
    weighted_credits = 0.0
    total_credits = 0.0
    compulsory_credits = 0.0
    elective_credits = 0.0

    qualitative_scores = {
        "优秀": 95.0,
        "良好": 85.0,
        "中等": 75.0,
        "及格": 65.0,
        "合格": 80.0,
        "通过": 80.0,
        "不及格": 0.0,
        "不合格": 0.0,
    }
    for row, hinted_type in rows:
        name = _value(row, SCORE_FIELD_ALIASES["name"])
        score_text = _value(row, SCORE_FIELD_ALIASES["score"])
        credit_text = _value(row, SCORE_FIELD_ALIASES["credit"])
        if not name or not score_text:
            continue
        type_value = hinted_type or _course_type(_value(row, SCORE_FIELD_ALIASES["type"]))
        credit = _float(credit_text)
        numeric_score = _float(score_text) or qualitative_scores.get(score_text, 0.0)
        total_credits += credit
        if type_value == "必修":
            compulsory_credits += credit
        else:
            elective_credits += credit
        if numeric_score > 0 and credit > 0:
            weighted_total += numeric_score * credit
            weighted_credits += credit
        scores.append(Score(
            name=name,
            score=score_text,
            credit=credit_text or "0",
            type=type_value,
            term=_term_ordinal(_value(row, SCORE_FIELD_ALIASES["term"]), account),
        ))

    degree_credit = _summary_number(payload, page_html, "degree_credit") or compulsory_credits
    required_degree_credit = _summary_number(payload, page_html, "required_degree_credit") or degree_credit
    non_degree_credit = _summary_number(payload, page_html, "non_degree_credit") or elective_credits
    required_non_degree_credit = (
        _summary_number(payload, page_html, "required_non_degree_credit") or non_degree_credit
    )
    total_credit = _summary_number(payload, page_html, "total_credit") or total_credits
    required_total_credit = _summary_number(payload, page_html, "required_total_credit") or total_credit
    average = weighted_total / weighted_credits if weighted_credits else 0.0
    gpa = _summary_value(payload, page_html, "gpa") or "0"
    return ScoreBoard(
        student_id=account,
        student_type="graduate",
        scores=scores,
        total_credit=(_display_number(required_total_credit), _display_number(total_credit)),
        compulsory_credit=(_display_number(required_degree_credit), _display_number(degree_credit)),
        elective_credit=(_display_number(required_non_degree_credit), _display_number(non_degree_credit)),
        cross_course_credit=("0", "0"),
        average_score=f"{average:.2f}" if average else "0",
        gpa=gpa,
    )


def _find_score_request(page_html: str, page_url: str) -> tuple[str, str] | None:
    for block in re.split(r"\$\s*\.\s*ajax\s*\(", page_html, flags=re.IGNORECASE)[1:]:
        block = block[:5000]
        match = re.search(r"url\s*:\s*['\"]([^'\"]+)['\"]", block, flags=re.IGNORECASE)
        if not match:
            continue
        raw_url = match.group(1)
        if "/xsxx/xscjcx_list" not in _decode_obfuscated_route(raw_url):
            continue
        method_match = re.search(r"type\s*:\s*['\"](get|post)['\"]", block, flags=re.IGNORECASE)
        method = method_match.group(1).upper() if method_match else "POST"
        return urljoin(page_url, raw_url), method
    return None


def _find_summary_requests(page_html: str, page_url: str) -> list[tuple[str, str, str]]:
    requests = []
    summary_markers = tuple(
        alias.lower()
        for aliases in SUMMARY_ALIASES.values()
        for alias in aliases
    )
    for block in re.split(r"\$\s*\.\s*ajax\s*\(", page_html, flags=re.IGNORECASE)[1:]:
        block = block[:20000]
        match = re.search(r"url\s*:\s*['\"]([^'\"]+)['\"]", block, flags=re.IGNORECASE)
        if not match:
            continue
        raw_url = match.group(1)
        decoded = _decode_obfuscated_route(raw_url).lower()
        block_lower = block.lower()
        has_summary_marker = any(marker in block_lower for marker in summary_markers)
        looks_like_score_query = "kccjcx" in decoded and any(
            marker in decoded for marker in ("list", "load", "query", "cx")
        )
        if not has_summary_marker and not looks_like_score_query:
            continue
        if any(marker in decoded for marker in ("print", "export", "download")):
            continue
        method_match = re.search(r"type\s*:\s*['\"](get|post)['\"]", block, flags=re.IGNORECASE)
        method = method_match.group(1).upper() if method_match else "POST"
        request = (urljoin(page_url, raw_url), method, decoded)
        if request not in requests:
            requests.append(request)
    return requests


def _find_summary_field_mapping(page_html: str) -> dict[str, str]:
    mapping = {}
    aliases = {
        alias.lower()
        for field_aliases in SUMMARY_ALIASES.values()
        for alias in field_aliases
    }
    for target in aliases:
        for marker in re.finditer(rf"#?{re.escape(target)}\b", page_html, flags=re.IGNORECASE):
            selector_context = page_html[max(0, marker.start() - 100):marker.end()]
            if "$(" not in selector_context and "getElementById" not in selector_context:
                continue
            nearby = page_html[marker.end():marker.end() + 500]
            value_match = re.search(
                r"(?:html|text|val)\s*\(\s*([^)]{1,300})\)",
                nearby,
                flags=re.IGNORECASE,
            )
            if not value_match:
                continue
            expression = value_match.group(1)
            properties = re.findall(
                r"(?:\.\s*|\[\s*['\"])([A-Za-z_$][A-Za-z0-9_$]*)",
                expression,
            )
            if properties:
                mapping[target] = properties[-1]
                break
    return mapping


def _payload_keys(payload) -> list[str]:
    keys = set()
    if isinstance(payload, dict):
        for key, value in payload.items():
            keys.add(str(key))
            keys.update(_payload_keys(value))
    elif isinstance(payload, list):
        for value in payload:
            keys.update(_payload_keys(value))
    return sorted(keys)


def _extract_summary_values(payload, field_mapping: dict[str, str] | None = None) -> dict[str, str]:
    values = {}
    field_mapping = field_mapping or {}
    for aliases in SUMMARY_ALIASES.values():
        for alias in aliases:
            source_alias = field_mapping.get(alias.lower(), alias)
            value = _payload_value(payload, (alias, source_alias))
            if value:
                values[alias] = value
                break
    return values


async def _load_score_source(session: HttpSessionHolder):
    async with session.to_aiohttp_session() as http_session:
        async with http_session.get(GMS_INDEX_URL) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to fetch graduate index page")
            index_html = await response.text()
            personal_page = _find_personal_page(index_html)
            score_page = _find_score_page(index_html)
        if not personal_page:
            raise ServiceUnavailableException("Graduate score page not found")

        async with http_session.get(personal_page) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to fetch graduate student page")
            page_html = await response.text()

        summary_html = ""
        summary_values = {}
        if score_page and score_page != personal_page:
            async with http_session.get(score_page) as response:
                if response.status == 200:
                    summary_html = await response.text()
                    logger.info(
                        "研究生成绩统计页面响应：url=%s, final_url=%s, length=%s",
                        score_page,
                        response.url,
                        len(summary_html),
                    )
                    summary_requests = _find_summary_requests(summary_html, str(response.url))
                    summary_field_mapping = _find_summary_field_mapping(summary_html)
                    logger.info(
                        "研究生成绩统计请求发现：count=%s, routes=%s, mapping=%s",
                        len(summary_requests),
                        [request[2] for request in summary_requests],
                        summary_field_mapping,
                    )
                    for summary_url, summary_method, decoded_route in summary_requests:
                        request_kwargs = {
                            "headers": {"X-Requested-With": "XMLHttpRequest"},
                        }
                        paging = {"page": "1", "rows": "1000"}
                        request_kwargs["params" if summary_method == "GET" else "data"] = paging
                        async with http_session.request(
                            summary_method,
                            summary_url,
                            **request_kwargs,
                        ) as summary_response:
                            summary_text = await summary_response.text()
                            candidate_payload = None
                            if summary_response.status != 200:
                                logger.warning(
                                    "研究生成绩统计接口请求失败：route=%s, status=%s",
                                    decoded_route,
                                    summary_response.status,
                                )
                                continue
                            try:
                                candidate_payload = json.loads(summary_text)
                            except json.JSONDecodeError:
                                summary_html += summary_text
                                candidate_values = {}
                            else:
                                candidate_values = _extract_summary_values(
                                    candidate_payload,
                                    summary_field_mapping,
                                )
                            logger.info(
                                "研究生成绩统计接口响应：route=%s, length=%s, keys=%s, fields=%s",
                                decoded_route,
                                len(summary_text),
                                _payload_keys(candidate_payload) if candidate_payload is not None else [],
                                sorted(candidate_values),
                            )
                            summary_values.update(candidate_values)
                            if all(
                                name in summary_values
                                for name in ("pjjd", "xszcjpm", "xwkpm", "gpapm", "zrs")
                            ):
                                break
                else:
                    logger.warning("研究生成绩统计页面请求失败：status=%s", response.status)

        score_request = _find_score_request(page_html, personal_page)
        if not score_request:
            raise ServiceUnavailableException("Graduate score request not found")
        score_url, method = score_request
        request_kwargs = {
            "headers": {"X-Requested-With": "XMLHttpRequest"},
        }
        paging = {"page": "1", "rows": "1000"}
        request_kwargs["params" if method == "GET" else "data"] = paging
        async with http_session.request(method, score_url, **request_kwargs) as response:
            if response.status in (401, 403):
                raise SessionInvalidException("Failed to fetch graduate scores")
            if response.status != 200:
                raise ServiceUnavailableException(
                    "Graduate EMS scores",
                    f"成绩接口返回 HTTP {response.status}",
                )
            response_text = await response.text()
            response_url = str(response.url)
        logger.info(
            "研究生成绩接口响应：method=%s, url=%s, final_url=%s, length=%s",
            method,
            score_url,
            response_url,
            len(response_text),
        )
        try:
            payload = json.loads(response_text)
        except json.JSONDecodeError as exc:
            raise ServiceUnavailableException(
                "Graduate EMS scores",
                "成绩接口未返回 JSON",
            ) from exc
    return {"courses": payload, "summary": summary_values}, page_html + summary_html


async def get_transcript_scoreboard(session: HttpSessionHolder) -> ScoreBoard:
    account = str(session.metadata.get("account", ""))
    payload, page_html = await _load_score_source(session)

    scoreboard = parse_score_payload(payload, account, page_html)
    try:
        info = await get_student_info(session)
        scoreboard.name = info.name
        scoreboard.major = info.major
        scoreboard.college = info.college
    except Exception:
        logger.warning("研究生成绩中的个人信息补全失败", exc_info=True)

    payload_keys = sorted({str(key) for rows in _iter_lists(payload) for row in rows if isinstance(row, dict) for key in row})
    source_rows = [row for row, _ in _find_score_rows(payload)]
    logger.info(
        "研究生成绩解析完成：rows=%s, scores=%s, keys=%s, types=%s, terms=%s, credits=%s",
        len(source_rows),
        len(scoreboard.scores),
        payload_keys[:80],
        sorted({_value(row, SCORE_FIELD_ALIASES["type"]) for row in source_rows}),
        sorted({_value(row, SCORE_FIELD_ALIASES["term"]) for row in source_rows}),
        sorted({_value(row, SCORE_FIELD_ALIASES["credit"]) for row in source_rows}),
    )
    return scoreboard


def _int_summary(payload, page_html: str, name: str) -> int:
    return int(_summary_number(payload, page_html, name))


async def get_rank_info(session: HttpSessionHolder, degree_only: bool = False) -> RankInfo:
    payload, page_html = await _load_score_source(session)
    scoreboard = parse_score_payload(
        payload,
        str(session.metadata.get("account", "")),
        page_html,
    )
    total_score_rank = _int_summary(payload, page_html, "total_score_rank")
    degree_course_rank = _int_summary(payload, page_html, "degree_course_rank")
    gpa_rank = _int_summary(payload, page_html, "gpa_rank")
    score_rank = _int_summary(payload, page_html, "score_rank")
    total_people = _int_summary(payload, page_html, "total_people")
    selected_rank = degree_course_rank if degree_only else (total_score_rank or score_rank)
    logger.info(
        "研究生成绩统计解析：gpa=%s, score_rank=%s, total_rank=%s, degree_rank=%s, "
        "gpa_rank=%s, total_people=%s, degree_only=%s",
        scoreboard.gpa,
        score_rank,
        total_score_rank,
        degree_course_rank,
        gpa_rank,
        total_people,
        degree_only,
    )
    return RankInfo(
        average_score=scoreboard.average_score,
        gpa=scoreboard.gpa,
        class_rank=selected_rank,
        major_rank=selected_rank,
        weighted_average_rank=degree_course_rank if degree_only else 0,
        terms=[],
        student_type="graduate",
        total_people=total_people,
        total_score_rank=total_score_rank or score_rank,
        degree_course_rank=degree_course_rank,
        gpa_rank=gpa_rank,
    )


async def total_rank_getter(session: HttpSessionHolder) -> RankInfo:
    return await get_rank_info(session, degree_only=False)


async def degree_rank_getter(session: HttpSessionHolder) -> RankInfo:
    return await get_rank_info(session, degree_only=True)
