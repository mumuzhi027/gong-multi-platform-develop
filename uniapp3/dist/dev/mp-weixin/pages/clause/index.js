"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = {
  __name: "index",
  setup(__props) {
    const type = common_vendor.ref("userClause");
    const title = common_vendor.ref("用户协议");
    const fallbackContent = {
      privacyPolicy: {
        heading: "隐私政策",
        sections: [
          ["信息收集", "为提供课表、成绩、考试和空教室服务，应用可能处理学号、姓名、专业及教务数据。"],
          ["信息使用", "相关信息仅用于展示校园服务内容、维持登录状态和改善使用体验。"],
          ["本地存储", "为支持离线查看，部分课程与学业数据会保存在当前设备的小程序存储空间中。"],
          ["联系我们", "如对个人信息处理有疑问，请通过项目维护方提供的渠道联系我们。"]
        ]
      },
      permissionsRequest: {
        heading: "权限申请与使用情况说明",
        sections: [
          ["网络权限", "用于登录并请求课表、考试、成绩、空教室等服务数据。"],
          ["存储能力", "使用小程序本地存储保存登录凭证、偏好设置和离线缓存。"],
          ["订阅消息", "仅在你主动授权后，用于发送课程或考试提醒；你可以随时在微信设置中关闭。"]
        ]
      },
      userClause: {
        heading: "用户协议",
        sections: [
          ["服务说明", "拱拱是校园信息辅助工具，展示内容以学校教务系统和相关数据源为准。"],
          ["账号安全", "请妥善保管个人账号与设备，不要将登录凭证提供给他人。"],
          ["合理使用", "不得利用本服务干扰学校系统、批量抓取数据或从事违反法律法规的行为。"],
          ["变更与终止", "服务内容可能因学校系统、接口策略或小程序平台规则调整。"]
        ]
      }
    };
    common_vendor.onLoad((query) => {
      var _a;
      type.value = query.type || "userClause";
      title.value = decodeURIComponent(query.title || ((_a = fallbackContent[type.value]) == null ? void 0 : _a.heading) || "条款");
      common_vendor.index.setNavigationBarTitle({ title: title.value });
    });
    function openOnline() {
      common_vendor.index.showToast({ title: "请先配置小程序业务域名", icon: "none" });
    }
    return (_ctx, _cache) => {
      var _a, _b;
      return {
        a: common_vendor.t(((_a = fallbackContent[type.value]) == null ? void 0 : _a.heading) || title.value),
        b: common_vendor.f(((_b = fallbackContent[type.value]) == null ? void 0 : _b.sections) || [], (section, k0, i0) => {
          return {
            a: common_vendor.t(section[0]),
            b: common_vendor.t(section[1]),
            c: section[0]
          };
        }),
        c: common_vendor.o(openOnline, "fc")
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-88e6c74f"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/clause/index.js.map
