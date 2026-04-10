"use strict";
const utils_api = require("../utils/api.js");
const getAllAttendants = () => {
  return utils_api.get("/api/attendants");
};
exports.getAllAttendants = getAllAttendants;
//# sourceMappingURL=../../.sourcemap/mp-weixin/api/attendant.js.map
