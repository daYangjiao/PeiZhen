"use strict";
const common_vendor = require("../common/vendor.js");
async function askMedicalQuestion(question) {
  try {
    const res = await $u.http.post("/ai/medical/qa", { question });
    return res.data;
  } catch (error) {
    common_vendor.index.__f__("error", "at api/aiaks.js:19", "AI 医疗问答请求失败:", error);
    return null;
  }
}
exports.askMedicalQuestion = askMedicalQuestion;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/aiaks.js.map
