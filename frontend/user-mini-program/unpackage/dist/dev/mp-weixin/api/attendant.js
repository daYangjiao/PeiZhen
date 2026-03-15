"use strict";
const utils_api = require("../utils/api.js");
const getRecommendedAttendants = () => {
  return utils_api.get("/attendant/recommended");
};
exports.getRecommendedAttendants = getRecommendedAttendants;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/attendant.js.map
