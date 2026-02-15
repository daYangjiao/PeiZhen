"use strict";
const common_vendor = require("../../common/vendor.js");
const stores_user = require("../../stores/user.js");
const api_user = require("../../api/user.js");
const _sfc_main = {
  __name: "edit-profile",
  setup(__props) {
    const userStore = common_vendor.ref(null);
    const isEditing = common_vendor.ref(false);
    const originalUserData = common_vendor.ref({
      avatar: "",
      name: "",
      username: "",
      sex: "",
      // 男/女
      age: null,
      phone: "",
      password: ""
    });
    const userForm = common_vendor.ref({
      avatar: "",
      name: "",
      username: "",
      sex: "",
      age: null,
      phone: "",
      password: ""
    });
    common_vendor.onMounted(() => {
      userStore.value = stores_user.useUserStore();
      userStore.value.restoreFromStorage();
      loadUserInfo();
    });
    const loadUserInfo = async () => {
      const localUserInfo = common_vendor.index.getStorageSync("userInfo");
      if (!localUserInfo || !localUserInfo.id) {
        common_vendor.index.showToast({ title: "请先登录", icon: "none" });
        common_vendor.index.navigateTo({ url: "/subpkg/auth/login" });
        return;
      }
      try {
        const response = await api_user.getUserInfo(localUserInfo.id);
        const userData = response.data || response;
        if (userData) {
          originalUserData.value = {
            id: userData.userId || userData.id || localUserInfo.id,
            userId: userData.userId || userData.id || localUserInfo.id,
            avatar: userData.avatar || "",
            name: userData.name || "",
            username: userData.username || "",
            sex: userData.sex || "",
            age: userData.age || null,
            phone: userData.phone || "",
            password: ""
          };
          userForm.value = { ...originalUserData.value, password: "" };
        } else {
          common_vendor.index.showToast({ title: "获取用户信息失败", icon: "none" });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/profile/edit-profile.vue:159", "获取用户信息失败:", error);
        common_vendor.index.showToast({ title: "网络错误", icon: "none" });
      }
    };
    const chooseAvatar = () => {
      common_vendor.index.chooseImage({
        count: 1,
        sizeType: ["compressed"],
        sourceType: ["album", "camera"],
        success: (res) => {
          const tempFilePath = res.tempFilePaths[0];
          userForm.value.avatar = tempFilePath;
          uploadUserAvatar(tempFilePath);
        }
      });
    };
    const uploadUserAvatar = async (filePath) => {
      try {
        common_vendor.index.showLoading({ title: "上传中..." });
        const response = await api_user.uploadAvatar(filePath);
        common_vendor.index.hideLoading();
        if (response.data && response.data.avatarUrl) {
          userForm.value.avatar = response.data.avatarUrl;
          common_vendor.index.showToast({
            title: "头像上传成功",
            icon: "success"
          });
        }
      } catch (error) {
        common_vendor.index.hideLoading();
        common_vendor.index.showToast({
          title: "头像上传失败",
          icon: "none"
        });
      }
    };
    const startEditing = () => {
      isEditing.value = true;
      userForm.value = {
        avatar: originalUserData.value.avatar || "",
        name: originalUserData.value.name || "",
        username: originalUserData.value.username || "",
        sex: originalUserData.value.sex || "",
        age: originalUserData.value.age || null,
        phone: originalUserData.value.phone || "",
        password: ""
      };
    };
    const cancelEditing = () => {
      isEditing.value = false;
      userForm.value = {
        avatar: "",
        name: "",
        username: "",
        sex: "",
        age: null,
        phone: "",
        password: ""
      };
    };
    const saveProfile = async () => {
      if (!userStore.value || !userStore.value.isLoggedIn) {
        common_vendor.index.showToast({
          title: "请先登录",
          icon: "none"
        });
        return;
      }
      try {
        common_vendor.index.showLoading({ title: "保存中..." });
        const updatedFields = {};
        let userId = null;
        if (userStore.value && userStore.value.userInfo) {
          userId = userStore.value.userInfo.userId || userStore.value.userInfo.id;
        }
        if (!userId && originalUserData.value) {
          userId = originalUserData.value.userId || originalUserData.value.id;
        }
        if (userId) {
          updatedFields.id = userId;
        } else {
          common_vendor.index.hideLoading();
          common_vendor.index.showToast({
            title: "无法获取用户ID，请重新登录",
            icon: "none"
          });
          return;
        }
        Object.keys(userForm.value).forEach((key) => {
          const newValue = userForm.value[key];
          const originalValue = originalUserData.value[key];
          if (key === "password" && (!newValue || newValue === "")) {
            return;
          }
          if (key === "avatar") {
            return;
          }
          let normalizedNewValue = newValue;
          let normalizedOriginalValue = originalValue;
          if (key !== "age") {
            normalizedNewValue = String(newValue || "");
            normalizedOriginalValue = String(originalValue || "");
          }
          if (normalizedNewValue !== normalizedOriginalValue && normalizedNewValue !== "") {
            let backendKey = key;
            if (key === "name") {
              backendKey = "realName";
            } else if (key === "sex") {
              backendKey = "gender";
            }
            if (key === "age" && newValue) {
              updatedFields[backendKey] = parseInt(newValue);
            } else {
              updatedFields[backendKey] = newValue;
            }
          }
        });
        const response = await api_user.updateUserInfo(updatedFields);
        common_vendor.index.hideLoading();
        if (response.code === 200) {
          const updatedUserInfo = {
            ...userStore.value.userInfo,
            ...updatedFields
          };
          userStore.value.setUserInfo(updatedUserInfo);
          await loadUserInfo();
          isEditing.value = false;
          common_vendor.index.showToast({
            title: "保存成功",
            icon: "success"
          });
        } else {
          common_vendor.index.showToast({
            title: response.message || "保存失败",
            icon: "none"
          });
        }
      } catch (error) {
        common_vendor.index.hideLoading();
        common_vendor.index.__f__("error", "at subpkg/profile/edit-profile.vue:349", "更新用户信息失败:", error);
        common_vendor.index.showToast({
          title: "保存失败，请重试",
          icon: "none"
        });
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: userForm.value.avatar || originalUserData.value.avatar || "/static/user-placeholder.png",
        b: common_vendor.o(($event) => isEditing.value ? chooseAvatar : null),
        c: isEditing.value
      }, isEditing.value ? {} : {}, {
        d: !isEditing.value
      }, !isEditing.value ? {
        e: common_vendor.t(originalUserData.value.name || "未设置")
      } : {
        f: originalUserData.value.name || "请输入姓名",
        g: userForm.value.name,
        h: common_vendor.o(($event) => userForm.value.name = $event.detail.value)
      }, {
        i: !isEditing.value
      }, !isEditing.value ? {
        j: common_vendor.t(originalUserData.value.username || "未设置")
      } : {
        k: originalUserData.value.username || "请输入用户名",
        l: userForm.value.username,
        m: common_vendor.o(($event) => userForm.value.username = $event.detail.value)
      }, {
        n: !isEditing.value
      }, !isEditing.value ? {
        o: common_vendor.t(originalUserData.value.sex || "未设置")
      } : {
        p: userForm.value.sex === "男" || !userForm.value.sex && originalUserData.value.sex === "男" ? 1 : "",
        q: common_vendor.o(($event) => userForm.value.sex = "男"),
        r: userForm.value.sex === "女" || !userForm.value.sex && originalUserData.value.sex === "女" ? 1 : "",
        s: common_vendor.o(($event) => userForm.value.sex = "女")
      }, {
        t: !isEditing.value
      }, !isEditing.value ? {
        v: common_vendor.t(originalUserData.value.age || "未设置")
      } : {
        w: originalUserData.value.age ? originalUserData.value.age + "" : "请输入年龄",
        x: userForm.value.age,
        y: common_vendor.o(($event) => userForm.value.age = $event.detail.value)
      }, {
        z: !isEditing.value
      }, !isEditing.value ? {
        A: common_vendor.t(originalUserData.value.phone || "未设置")
      } : {
        B: originalUserData.value.phone || "请输入手机号",
        C: userForm.value.phone,
        D: common_vendor.o(($event) => userForm.value.phone = $event.detail.value)
      }, {
        E: isEditing.value
      }, isEditing.value ? {
        F: userForm.value.password,
        G: common_vendor.o(($event) => userForm.value.password = $event.detail.value)
      } : {}, {
        H: !isEditing.value
      }, !isEditing.value ? {
        I: common_vendor.o(startEditing)
      } : {
        J: common_vendor.o(cancelEditing),
        K: common_vendor.o(saveProfile)
      });
    };
  }
};
wx.createPage(_sfc_main);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/profile/edit-profile.js.map
