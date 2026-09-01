"use strict";
const common_vendor = require("../common/vendor.js");
if (!Math) {
  AuthorizationDialog();
}
const AuthorizationDialog = () => "./AuthorizationDialog.js";
const _sfc_main = {
  __name: "AppNav",
  props: {
    current: { type: String, required: true }
  },
  setup(__props) {
    const props = __props;
    const items = [
      { key: "home", label: "首页", icon: "home", url: "/pages/home/index" },
      { key: "classroom", label: "空教室", icon: "classroom", url: "/pages/classroom/index" },
      { key: "academic", label: "成绩单", icon: "grade", url: "/pages/academic/index" }
    ];
    function navigate(item) {
      if (item.key !== props.current)
        common_vendor.index.redirectTo({ url: item.url });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.f(items, (item, k0, i0) => {
          return {
            a: `/static/icons/${item.icon}${item.key === __props.current ? "-active" : ""}.png`,
            b: common_vendor.t(item.label),
            c: item.key,
            d: item.key === __props.current ? 1 : "",
            e: common_vendor.o(($event) => navigate(item), item.key)
          };
        })
      };
    };
  }
};
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-d80c7d29"]]);
wx.createComponent(Component);
//# sourceMappingURL=../../.sourcemap/mp-weixin/components/AppNav.js.map
