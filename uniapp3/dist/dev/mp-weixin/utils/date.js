"use strict";
const WEEKDAYS = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
function formatDate(date = /* @__PURE__ */ new Date()) {
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())}`;
}
function weekdayName(date = /* @__PURE__ */ new Date()) {
  return WEEKDAYS[date.getDay()];
}
function calculateWeek(calendar, date = /* @__PURE__ */ new Date()) {
  if (!(calendar == null ? void 0 : calendar.start))
    return 0;
  const start = /* @__PURE__ */ new Date(`${calendar.start}T00:00:00`);
  const current = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const days = Math.trunc((current - start) / 864e5);
  return Math.max(0, Math.trunc(days / 7) + 1);
}
function parseWeeks(value = "") {
  const result = /* @__PURE__ */ new Set();
  String(value).split(",").map((item) => item.trim()).filter(Boolean).forEach((part) => {
    const [from, to] = part.split("-").map(Number);
    if (Number.isFinite(from) && Number.isFinite(to)) {
      for (let week = from; week <= to; week += 1)
        result.add(week);
    } else if (Number.isFinite(from)) {
      result.add(from);
    }
  });
  return result;
}
function academicTermLabel(term) {
  const value = Number(term);
  if (!value)
    return "未知学期";
  const year = Math.ceil(value / 2);
  const names = ["零", "一", "二", "三", "四", "五", "六"];
  return `大${names[year] || year}${value % 2 ? "上" : "下"}`;
}
function addDays(date, amount) {
  const result = new Date(date);
  result.setDate(result.getDate() + amount);
  return result;
}
function weekDates(calendar, week) {
  if (!(calendar == null ? void 0 : calendar.start))
    return Array.from({ length: 7 }, (_, index) => addDays(/* @__PURE__ */ new Date(), index - ((/* @__PURE__ */ new Date()).getDay() + 6) % 7));
  const start = /* @__PURE__ */ new Date(`${calendar.start}T00:00:00`);
  return Array.from({ length: 7 }, (_, index) => addDays(start, (Number(week) - 1) * 7 + index));
}
function dateLabel(offset = 0) {
  const date = /* @__PURE__ */ new Date();
  date.setDate(date.getDate() + offset);
  return `${date.getMonth() + 1}月${date.getDate()}日`;
}
exports.academicTermLabel = academicTermLabel;
exports.calculateWeek = calculateWeek;
exports.dateLabel = dateLabel;
exports.formatDate = formatDate;
exports.parseWeeks = parseWeeks;
exports.weekDates = weekDates;
exports.weekdayName = weekdayName;
//# sourceMappingURL=../../.sourcemap/mp-weixin/utils/date.js.map
