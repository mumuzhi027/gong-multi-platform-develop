import re
import logging
import base64
import json
from urllib.parse import urljoin

import bs4

from xtu_ems.common.exception import SessionInvalidException
from xtu_ems.common.model import StudentBasicInfo
from xtu_ems.common.sess import HttpSessionHolder

GMS_INDEX_URL = "https://gmsstu.xtu.edu.cn/index"
logger = logging.getLogger(__name__)
PERSONAL_PAGE_TITLES = ("个人基本信息", "基本信息", "个人信息", "学籍信息查询")

FIELD_IDS = {
    "student_id": ("col_xh", "xh", "studentId", "student_id"),
    "name": ("col_xm", "xm", "userName", "username"),
    "gender": ("col_xbm", "xbm", "xb", "gender"),
    "birthday": ("col_csrq", "csrq", "birthday"),
    "major": ("col_zyh_id", "zymc", "zy", "major"),
    "class_": ("col_bh_id", "bjmc", "bj", "className"),
    "entrance_day": ("col_rxrq", "rxrq", "entranceDate"),
    "college": ("col_jg_id", "xymc", "yxmc", "college"),
}

JSON_FIELD_ALIASES = {
    "student_id": ("student_id", "studentid", "xh", "xuehao", "学号"),
    "name": ("name", "xm", "xsxm", "xingming", "姓名"),
    "gender": ("gender", "xb", "xbm", "xbmc", "性别"),
    "birthday": ("birthday", "csrq", "出生日期", "出生年月"),
    "major": ("major", "zy", "zymc", "zym", "专业", "专业名称"),
    "class_": ("class_", "classname", "bj", "bjmc", "班级", "班级名称"),
    "entrance_day": ("entrance_day", "entrancedate", "rxrq", "入学日期", "入学时间"),
    "college": ("college", "xy", "xymc", "yxmc", "dwmc", "培养单位", "学院"),
}

FIELD_LABELS = {
    "student_id": ("学号",),
    "name": ("姓名",),
    "gender": ("性别",),
    "birthday": ("出生日期", "出生年月"),
    "major": ("专业名称", "专业"),
    "class_": ("班级名称", "班级"),
    "entrance_day": ("入学日期", "入学时间"),
    "college": ("培养单位", "所属学院", "学院"),
}

ALL_FIELD_LABELS = {
    label
    for labels in FIELD_LABELS.values()
    for label in labels
} | {"专项计划"}


def _clean(value: str) -> str:
    return re.sub(r"\s+", " ", value or "").strip(" ：:\t\r\n")


def _element_value(element) -> str:
    if element is None:
        return ""
    if getattr(element, "name", "") in ("input", "select"):
        return _clean(element.get("value", ""))
    if hasattr(element, "get_text"):
        return _clean(element.get_text(" ", strip=True))
    return _clean(str(element))


def _usable_value(value: str, label_text: str = "") -> str:
    value = _clean(value)
    if (
        not value
        or len(value) > 100
        or value == label_text
        or value.rstrip("：:") in ALL_FIELD_LABELS
        or sum(label in value for label in ALL_FIELD_LABELS) >= 2
    ):
        return ""
    return value


def _value_by_id(soup: bs4.BeautifulSoup, ids: tuple[str, ...]) -> str:
    for element_id in ids:
        element = soup.find(id=element_id) or soup.find(attrs={"name": element_id})
        if element:
            value = _usable_value(_element_value(element))
            if value:
                return value
    return ""


def _value_by_label(soup: bs4.BeautifulSoup, labels: tuple[str, ...]) -> str:
    for text_node in soup.find_all(string=True):
        label_text = _clean(str(text_node))
        matched_label = next((label for label in labels if label_text.rstrip("：:") == label), None)
        if not matched_label:
            continue
        element = text_node.parent

        # label[for] -> input/select，是研究生系统表单最稳定的关联方式。
        target_id = element.get("for") if getattr(element, "name", "") == "label" else None
        if target_id:
            value = _usable_value(_element_value(soup.find(id=target_id)), label_text)
            if value:
                return value

        # table: 当前标题单元格的下一个单元格。
        cell = element.find_parent(["td", "th"])
        if cell:
            value = _usable_value(_element_value(cell.find_next_sibling(["td", "th"])), label_text)
            if value:
                return value

        # dl: <dt>姓名</dt><dd>张三</dd>。
        term = element.find_parent("dt")
        if term:
            value = _usable_value(_element_value(term.find_next_sibling("dd")), label_text)
            if value:
                return value

        # 常见 form-group 内的输入框或静态值；只在同一容器内查找，避免串到下一字段。
        container = element.find_parent(
            class_=lambda classes: classes and any(
                token in {"form-group", "form-item", "control-group", "info-item"}
                for token in (classes if isinstance(classes, list) else str(classes).split())
            )
        )
        if container:
            candidate = container.find(["input", "select", "textarea"])
            if not candidate:
                candidate = container.find(
                    class_=lambda classes: classes and any(
                        token in {"form-control-static", "value", "info-value", "field-value"}
                        for token in (classes if isinstance(classes, list) else str(classes).split())
                    )
                )
            value = _usable_value(_element_value(candidate), label_text)
            if value:
                return value

        # 兼容没有固定 class 的旧式表单：从最小祖先开始，仅接受“恰好一个”非空控件。
        ancestor = element.parent
        for _ in range(4):
            if not ancestor:
                break
            controls = [
                control
                for control in ancestor.find_all(["input", "select", "textarea"])
                if _usable_value(_element_value(control), label_text)
            ]
            if len(controls) == 1:
                return _usable_value(_element_value(controls[0]), label_text)
            if len(controls) > 1:
                break
            ancestor = ancestor.parent
    return ""


def parse_personal_info(html: str, account: str = "") -> StudentBasicInfo:
    soup = bs4.BeautifulSoup(html, "html.parser")
    values = {}
    for field, ids in FIELD_IDS.items():
        values[field] = _value_by_id(soup, ids) or _value_by_label(soup, FIELD_LABELS[field])

    if not re.fullmatch(r"[A-Za-z0-9_-]{5,32}", values["student_id"]):
        values["student_id"] = account
    if len(values["name"]) > 24 or "：" in values["name"] or ":" in values["name"]:
        values["name"] = ""
    return StudentBasicInfo(**values)


def _decode_obfuscated_route(raw_url: str) -> str:
    encoded = raw_url.split("?", 1)[0].strip("/")
    try:
        encoded += "=" * ((4 - len(encoded) % 4) % 4)
        return base64.urlsafe_b64decode(encoded).decode("utf-8", errors="ignore")
    except (ValueError, UnicodeDecodeError):
        return ""


def _find_basic_info_request(page_html: str, page_url: str) -> tuple[str, str] | None:
    for block in re.split(r"\$\s*\.\s*ajax\s*\(", page_html, flags=re.IGNORECASE)[1:]:
        block = block[:5000]
        match = re.search(r"url\s*:\s*['\"]([^'\"]+)['\"]", block, flags=re.IGNORECASE)
        if not match:
            continue
        raw_url = match.group(1)
        decoded = _decode_obfuscated_route(raw_url)
        if "/xsxx/xsxx_jbxx" not in decoded:
            continue
        method_match = re.search(r"type\s*:\s*['\"](get|post)['\"]", block, flags=re.IGNORECASE)
        method = method_match.group(1).upper() if method_match else "GET"
        return urljoin(page_url, raw_url), method
    return None


def _iter_dicts(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from _iter_dicts(child)
    elif isinstance(value, list):
        for child in value:
            yield from _iter_dicts(child)


def _scalar_text(value) -> str:
    if value is None or isinstance(value, (dict, list)):
        return ""
    return _clean(str(value))


def parse_personal_payload(payload, account: str = "") -> StudentBasicInfo:
    best_values = {}
    best_score = -1
    labeled_values = {}
    for item in _iter_dicts(payload):
        normalized = {str(key).lower(): value for key, value in item.items()}
        values = {}
        for field, aliases in JSON_FIELD_ALIASES.items():
            values[field] = next(
                (
                    _usable_value(_scalar_text(normalized[alias.lower()]))
                    for alias in aliases
                    if alias.lower() in normalized and _usable_value(_scalar_text(normalized[alias.lower()]))
                ),
                "",
            )
        score = sum(bool(value) for value in values.values())
        if score > best_score:
            best_values = values
            best_score = score

        # 兼容 [{"label":"姓名", "value":"张三"}, ...] 结构。
        row_label = next(
            (_scalar_text(normalized[key]) for key in ("label", "title", "mc", "zdmc", "fieldname") if key in normalized),
            "",
        ).rstrip("：:")
        row_value = next(
            (_scalar_text(normalized[key]) for key in ("value", "val", "nr", "displayvalue", "fieldvalue") if key in normalized),
            "",
        )
        if row_label and row_value:
            for field, labels in FIELD_LABELS.items():
                if row_label in labels:
                    labeled_values[field] = _usable_value(row_value, row_label)

    best_values = best_values or {field: "" for field in JSON_FIELD_ALIASES}
    for field, value in labeled_values.items():
        best_values[field] = value or best_values.get(field, "")
    if not re.fullmatch(r"[A-Za-z0-9_-]{5,32}", best_values.get("student_id", "")):
        best_values["student_id"] = account
    return StudentBasicInfo(**best_values)


def _merge_info(primary: StudentBasicInfo, secondary: StudentBasicInfo) -> StudentBasicInfo:
    return StudentBasicInfo(**{
        field: getattr(primary, field) or getattr(secondary, field)
        for field in StudentBasicInfo.model_fields
    })


def _find_personal_page(index_html: str) -> str | None:
    soup = bs4.BeautifulSoup(index_html, "html.parser")
    for title in PERSONAL_PAGE_TITLES:
        link = soup.find("a", attrs={"title": title}) or soup.find("a", string=lambda value: value and title in value)
        if link and link.get("href"):
            return urljoin(GMS_INDEX_URL, link["href"])
    return None


def _log_page_structure(page_html: str) -> None:
    """Log field names and request URLs without logging students' actual values."""
    soup = bs4.BeautifulSoup(page_html, "html.parser")
    controls = []
    for element in soup.find_all(["input", "select", "textarea"]):
        controls.append({
            "tag": element.name,
            "id": element.get("id", ""),
            "name": element.get("name", ""),
            "has_value": bool(_element_value(element)),
        })
    forms = [
        {"method": form.get("method", "get"), "action": form.get("action", "")}
        for form in soup.find_all("form")
    ]
    ajax_urls = list(dict.fromkeys(re.findall(
        r"url\s*:\s*['\"]([^'\"]+)['\"]",
        page_html,
        flags=re.IGNORECASE,
    )))
    logger.info(
        "研究生个人信息页面结构：forms=%s, controls=%s, ajax_urls=%s",
        forms[:10],
        controls[:40],
        ajax_urls[:20],
    )


async def get_student_info(session: HttpSessionHolder) -> StudentBasicInfo:
    account = str(session.metadata.get("account", ""))
    async with session.to_aiohttp_session() as http_session:
        async with http_session.get(GMS_INDEX_URL) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to fetch graduate index page")
            index_html = await response.text()

        personal_page = _find_personal_page(index_html)
        if not personal_page:
            # 部分版本直接把姓名/学号写在首页，仍可解析；学号至少由登录账号兜底。
            info = parse_personal_info(index_html, account)
            logger.info("研究生个人信息使用首页解析：name=%s", bool(info.name))
            return info

        async with http_session.get(personal_page) as response:
            if response.status != 200:
                raise SessionInvalidException("Failed to fetch graduate personal info page")
            page_html = await response.text()
            info = parse_personal_info(page_html, account)

            basic_request = _find_basic_info_request(page_html, personal_page)
            if basic_request:
                basic_url, method = basic_request
                async with http_session.request(
                    method,
                    basic_url,
                    headers={"X-Requested-With": "XMLHttpRequest"},
                ) as basic_response:
                    basic_text = await basic_response.text()
                    if basic_response.status == 200:
                        try:
                            basic_payload = json.loads(basic_text)
                            basic_info = parse_personal_payload(basic_payload, account)
                            if not basic_info.name or not basic_info.major:
                                payload_keys = sorted({
                                    str(key)
                                    for item in _iter_dicts(basic_payload)
                                    for key in item.keys()
                                })
                                logger.info(
                                    "研究生基本信息接口结构：length=%s, keys=%s",
                                    len(basic_text),
                                    payload_keys[:80],
                                )
                        except json.JSONDecodeError:
                            basic_info = parse_personal_info(basic_text, account)
                        info = _merge_info(basic_info, info)
                    else:
                        logger.warning(
                            "研究生基本信息接口请求失败：status=%s, method=%s",
                            basic_response.status,
                            method,
                        )
            logger.info("研究生个人信息解析完成：basic_api=%s, name=%s, major=%s", bool(basic_request), bool(info.name), bool(info.major))
            if not info.name or not info.major:
                _log_page_structure(page_html)
            return info
