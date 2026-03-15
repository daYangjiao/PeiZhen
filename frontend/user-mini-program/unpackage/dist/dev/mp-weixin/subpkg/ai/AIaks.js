"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const api_aiaks = require("../../api/aiaks.js");
if (!Array) {
  const _component_path = common_vendor.resolveComponent("path");
  const _component_line = common_vendor.resolveComponent("line");
  const _component_svg = common_vendor.resolveComponent("svg");
  const _component_rect = common_vendor.resolveComponent("rect");
  const _component_circle = common_vendor.resolveComponent("circle");
  const _component_polyline = common_vendor.resolveComponent("polyline");
  (_component_path + _component_line + _component_svg + _component_rect + _component_circle + _component_polyline)();
}
const _sfc_main = {
  __name: "AIaks",
  setup(__props) {
    const AIAvatar = utils_api.getBackendImageUrl("ai-avatar.png");
    const messages = common_vendor.ref([
      {
        type: "ai",
        text: "您好！我是您的智能医疗助手，很高兴为您提供帮助。请问有什么健康问题需要咨询吗？"
      }
    ]);
    const userInput = common_vendor.ref("");
    const selectedTag = common_vendor.ref("");
    const suggestions = [
      "紧张性头痛",
      "过敏性咳嗽",
      "上呼吸道感染",
      "支气管炎",
      "胃炎",
      "鼻炎",
      "咽炎",
      "结膜炎"
    ];
    const messagesContainer = common_vendor.ref(null);
    const scrollToBottom = () => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
      }
    };
    const selectTag = (tag) => {
      selectedTag.value = tag;
      userInput.value = tag;
    };
    const sendMessage = async () => {
      if (!userInput.value.trim())
        return;
      messages.value.push({
        type: "user",
        text: userInput.value
      });
      const userMsg = userInput.value;
      userInput.value = "";
      scrollToBottom();
      messages.value.push({
        type: "ai",
        text: "正在思考..."
      });
      scrollToBottom();
      const result = await api_aiaks.askMedicalQuestion(userMsg);
      messages.value.pop();
      if (result) {
        messages.value.push({
          type: "ai",
          text: result.answer
          // 注意：这里只取 answer
        });
      } else {
        messages.value.push({
          type: "ai",
          text: "抱歉，当前无法回答您的问题，请稍后再试。"
        });
      }
      scrollToBottom();
    };
    const toggleMic = () => alert("语音输入");
    const addImage = () => alert("上传图片");
    const attachFile = () => alert("上传文件");
    common_vendor.onMounted(() => {
      scrollToBottom();
    });
    return (_ctx, _cache) => {
      return {
        a: common_vendor.f(messages.value, (msg, index, i0) => {
          return common_vendor.e({
            a: msg.type === "ai"
          }, msg.type === "ai" ? {
            b: common_vendor.unref(AIAvatar),
            c: msg.text
          } : {
            d: common_vendor.t(msg.text),
            e: common_assets._imports_0$6
          }, {
            f: index
          });
        }),
        b: common_vendor.f(suggestions, (tag, index, i0) => {
          return {
            a: common_vendor.t(tag),
            b: index,
            c: common_vendor.o(($event) => selectTag(tag), index),
            d: common_vendor.n(selectedTag.value === tag ? "selected" : "")
          };
        }),
        c: common_vendor.o(sendMessage),
        d: userInput.value,
        e: common_vendor.o(($event) => userInput.value = $event.detail.value),
        f: common_vendor.p({
          d: "M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"
        }),
        g: common_vendor.p({
          d: "M19 10v2a7 7 0 0 1-14 0v-2"
        }),
        h: common_vendor.p({
          x1: "12",
          y1: "19",
          x2: "12",
          y2: "23"
        }),
        i: common_vendor.p({
          x1: "8",
          y1: "23",
          x2: "16",
          y2: "23"
        }),
        j: common_vendor.p({
          xmlns: "http://www.w3.org/2000/svg",
          width: "16",
          height: "16",
          viewBox: "0 0 24 24",
          fill: "none",
          stroke: "currentColor",
          ["stroke-width"]: "2",
          ["stroke-linecap"]: "round",
          ["stroke-linejoin"]: "round"
        }),
        k: common_vendor.o(toggleMic),
        l: common_vendor.p({
          x: "3",
          y: "3",
          width: "18",
          height: "18",
          rx: "2",
          ry: "2"
        }),
        m: common_vendor.p({
          cx: "8.5",
          cy: "8.5",
          r: "1.5"
        }),
        n: common_vendor.p({
          points: "21 15 16 10 5 21"
        }),
        o: common_vendor.p({
          xmlns: "http://www.w3.org/2000/svg",
          width: "18",
          height: "18",
          viewBox: "0 0 24 24",
          fill: "none",
          stroke: "currentColor",
          ["stroke-width"]: "2",
          ["stroke-linecap"]: "round",
          ["stroke-linejoin"]: "round"
        }),
        p: common_vendor.o(addImage),
        q: common_vendor.p({
          d: "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"
        }),
        r: common_vendor.p({
          points: "7 10 12 15 17 10"
        }),
        s: common_vendor.p({
          x1: "12",
          y1: "15",
          x2: "12",
          y2: "3"
        }),
        t: common_vendor.p({
          xmlns: "http://www.w3.org/2000/svg",
          width: "18",
          height: "18",
          viewBox: "0 0 24 24",
          fill: "none",
          stroke: "currentColor",
          ["stroke-width"]: "2",
          ["stroke-linecap"]: "round",
          ["stroke-linejoin"]: "round"
        }),
        v: common_vendor.o(attachFile),
        w: common_vendor.o(sendMessage)
      };
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-95018511"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/ai/AIaks.js.map
