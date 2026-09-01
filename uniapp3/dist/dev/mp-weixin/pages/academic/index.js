"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_auth = require("../../stores/auth.js");
const stores_campus = require("../../stores/campus.js");
const utils_date = require("../../utils/date.js");
if (!Math) {
  (StateView + AppNav)();
}
const AppNav = () => "../../components/AppNav.js";
const StateView = () => "../../components/StateView.js";
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const auth = stores_auth.useAuthStore();
    const campus = stores_campus.useCampusStore();
    const tab = common_vendor.ref("overview");
    const scoreType = common_vendor.ref("major");
    const statisticsType = common_vendor.ref("all");
    const selectedTerm = common_vendor.ref(null);
    const scoreData = common_vendor.computed(() => {
      var _a;
      return ((_a = campus.academic) == null ? void 0 : _a[scoreType.value]) || null;
    });
    const majorScore = common_vendor.computed(() => {
      var _a;
      return ((_a = campus.academic) == null ? void 0 : _a.major) || null;
    });
    const rankData = common_vendor.computed(() => {
      var _a, _b;
      return statisticsType.value === "all" ? (_a = campus.academic) == null ? void 0 : _a.rank : (_b = campus.academic) == null ? void 0 : _b.compulsoryRank;
    });
    const scoreMap = common_vendor.computed(() => {
      var _a;
      return (((_a = scoreData.value) == null ? void 0 : _a.scores) || []).reduce((map, score) => {
        var _a2;
        map[_a2 = score.term] || (map[_a2] = []);
        map[score.term].push(score);
        return map;
      }, {});
    });
    const terms = common_vendor.computed(() => Object.keys(scoreMap.value).map(Number).sort((a, b) => b - a));
    const currentScores = common_vendor.computed(() => {
      var _a;
      return ((_a = scoreMap.value) == null ? void 0 : _a[selectedTerm.value]) || [];
    });
    const scoreGroups = common_vendor.computed(() => [
      ["必修", currentScores.value.filter((item) => item.type === "必修")],
      ["选修", currentScores.value.filter((item) => item.type === "选修")],
      ["跨学科选修", currentScores.value.filter((item) => item.type === "跨学科选修")]
    ].filter(([, list]) => list.length));
    function field(source, snake, camel) {
      return (source == null ? void 0 : source[snake]) ?? (source == null ? void 0 : source[camel]);
    }
    function valueOrDash(value) {
      return value !== void 0 && value !== null && String(value).trim() && String(value) !== "0" ? value : "-";
    }
    function rankOrDash(value) {
      return Number(value) > 0 ? value : "-";
    }
    function creditText(values) {
      return Array.isArray(values) ? `${values[1] || 0}/${values[0] || 0}` : "-/-";
    }
    function creditPercent(values) {
      if (!Array.isArray(values) || !Number(values[0]))
        return "0%";
      return `${Math.min(100, Number(values[1] || 0) / Number(values[0]) * 100)}%`;
    }
    const overviewRows = common_vendor.computed(() => {
      const data = scoreData.value;
      if (scoreType.value === "minor")
        return [
          { label: "绩点", value: valueOrDash(data == null ? void 0 : data.gpa) },
          { label: "总学分", credits: field(data, "total_credit", "totalCredit") },
          { label: "平均成绩", value: valueOrDash(field(data, "average_score", "averageScore")) }
        ];
      return [
        { label: "绩点", value: valueOrDash(data == null ? void 0 : data.gpa) },
        { label: "平均成绩", value: valueOrDash(field(data, "average_score", "averageScore")) },
        { label: "总学分", credits: field(data, "total_credit", "totalCredit") },
        { label: "必修学分", credits: field(data, "compulsory_credit", "compulsoryCredit") },
        { label: "选修学分", credits: field(data, "elective_credit", "electiveCredit") },
        { label: "跨学科选修学分", credits: field(data, "cross_course_credit", "crossCourseCredit") }
      ];
    });
    function numericScore(value) {
      const number = Number(value);
      if (Number.isFinite(number))
        return number;
      return { 优: 95, 优秀: 95, 良: 93, 良好: 93, 中: 73, 中等: 73, 及格: 62, 合格: 62, 不及格: 30, 不合格: 30 }[String(value)];
    }
    const weightedAverage = common_vendor.computed(() => {
      var _a;
      let total = 0;
      let credits = 0;
      (((_a = majorScore.value) == null ? void 0 : _a.scores) || []).filter((item) => item.type === "必修").forEach((item) => {
        const score = numericScore(item.score);
        const credit = Number(item.credit);
        if (Number.isFinite(score) && Number.isFinite(credit)) {
          total += score * credit;
          credits += credit;
        }
      });
      return credits ? (total / credits).toFixed(2) : "-";
    });
    function weightedFor(scores) {
      let total = 0;
      let credits = 0;
      scores.forEach((item) => {
        const score = numericScore(item.score);
        const credit = Number(item.credit);
        if (Number.isFinite(score) && Number.isFinite(credit) && credit > 0) {
          total += score * credit;
          credits += credit;
        }
      });
      return credits ? (total / credits).toFixed(2) : "-";
    }
    const statisticsRows = common_vendor.computed(() => {
      const rank = rankData.value;
      const rows = [
        ["平均学分绩点", valueOrDash(rank == null ? void 0 : rank.gpa)],
        ["平均成绩", valueOrDash(field(rank, "average_score", "averageScore"))],
        ["班级排名", rankOrDash(field(rank, "class_rank", "classRank"))],
        ["年级专业排名", rankOrDash(field(rank, "major_rank", "majorRank"))]
      ];
      if (statisticsType.value === "required")
        rows.push(
          ["加权平均分", weightedAverage.value],
          ["加权平均成绩排名", rankOrDash(field(rank, "weighted_average_rank", "weightedAverageRank"))]
        );
      return rows;
    });
    common_vendor.watch([terms, scoreType], () => {
      selectedTerm.value = terms.value[0] || null;
    }, { immediate: true });
    common_vendor.onLoad(async () => {
      var _a;
      if (!auth.authenticated)
        return common_vendor.index.reLaunch({ url: "/pages/login/index" });
      if (!((_a = campus.academic) == null ? void 0 : _a.major) && !auth.offline)
        await refresh(false);
    });
    common_vendor.onPullDownRefresh(async () => {
      await refresh(true);
      common_vendor.index.stopPullDownRefresh();
    });
    async function refresh(force = false) {
      if (auth.offline)
        return common_vendor.index.showToast({ title: "离线模式下无法刷新", icon: "none" });
      await campus.loadAcademic(force).catch((error) => {
        if (!error.authExpired)
          common_vendor.index.showToast({ title: error.message, icon: "none" });
      });
    }
    return (_ctx, _cache) => {
      var _a, _b, _c, _d, _e;
      return common_vendor.e({
        a: tab.value === "overview" ? 1 : "",
        b: common_vendor.o(($event) => tab.value = "overview", "e9"),
        c: tab.value === "scores" ? 1 : "",
        d: common_vendor.o(($event) => tab.value = "scores", "82"),
        e: tab.value === "scores" ? 1 : "",
        f: tab.value === "overview"
      }, tab.value === "overview" ? {
        g: common_vendor.f(overviewRows.value, (row, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(row.label),
            b: common_vendor.t(row.credits ? creditText(row.credits) : row.value),
            c: row.credits
          }, row.credits ? {
            d: creditPercent(row.credits)
          } : {}, {
            e: row.label,
            f: common_vendor.n({
              credit: row.credits
            })
          });
        }),
        h: scoreType.value === "major" ? 1 : "",
        i: common_vendor.o(($event) => scoreType.value = "major", "1f"),
        j: scoreType.value === "minor" ? 1 : "",
        k: common_vendor.o(($event) => scoreType.value = "minor", "fe"),
        l: common_vendor.t(valueOrDash((_a = majorScore.value) == null ? void 0 : _a.cet4)),
        m: common_vendor.t(valueOrDash((_b = majorScore.value) == null ? void 0 : _b.cet6)),
        n: statisticsType.value === "required" ? 1 : "",
        o: common_vendor.o(($event) => statisticsType.value = "required", "8e"),
        p: statisticsType.value === "all" ? 1 : "",
        q: common_vendor.o(($event) => statisticsType.value = "all", "fd"),
        r: common_vendor.f(statisticsRows.value, (row, k0, i0) => {
          return {
            a: common_vendor.t(row[0]),
            b: common_vendor.t(row[1]),
            c: row[0]
          };
        })
      } : common_vendor.e({
        s: scoreType.value === "major" ? 1 : "",
        t: common_vendor.o(($event) => scoreType.value = "major", "4c"),
        v: scoreType.value === "minor" ? 1 : "",
        w: common_vendor.o(($event) => scoreType.value = "minor", "af"),
        x: common_vendor.f(terms.value, (term, k0, i0) => {
          return {
            a: common_vendor.t(common_vendor.unref(utils_date.academicTermLabel)(term)),
            b: term,
            c: common_vendor.n({
              active: term === selectedTerm.value
            }),
            d: common_vendor.o(($event) => selectedTerm.value = term, term)
          };
        }),
        y: selectedTerm.value
      }, selectedTerm.value ? {
        z: common_vendor.t(common_vendor.unref(utils_date.academicTermLabel)(selectedTerm.value))
      } : {}, {
        A: common_vendor.f(scoreGroups.value, (group, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(group[0]),
            b: common_vendor.f(group[1], (score, k1, i1) => {
              return {
                a: common_vendor.t(score.name),
                b: common_vendor.t(score.score),
                c: common_vendor.t(score.credit),
                d: `${group[0]}-${score.name}`
              };
            }),
            c: group[0] === "必修"
          }, group[0] === "必修" ? {
            d: common_vendor.t(weightedFor(group[1]))
          } : {}, {
            e: group[0]
          });
        }),
        B: !currentScores.value.length
      }, !currentScores.value.length ? {} : {}), {
        C: common_vendor.o(($event) => refresh(true), "b3"),
        D: common_vendor.p({
          loading: common_vendor.unref(campus).loading.academic && !((_c = common_vendor.unref(campus).academic) == null ? void 0 : _c.major),
          error: common_vendor.unref(campus).errors.academic,
          empty: !((_d = common_vendor.unref(campus).academic) == null ? void 0 : _d.major) && !((_e = common_vendor.unref(campus).academic) == null ? void 0 : _e.minor),
          ["empty-text"]: "暂无成绩数据"
        }),
        E: common_vendor.p({
          current: "academic"
        })
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-145009ce"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/academic/index.js.map
