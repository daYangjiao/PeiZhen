"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const utils_api = require("../../utils/api.js");
const utils_chatWebsocket = require("../../utils/chat-websocket.js");
const pageSize = 20;
const _sfc_main = {
  __name: "chat",
  setup(__props) {
    var _a;
    const currentUserId = common_vendor.ref(((_a = common_vendor.index.getStorageSync("userInfo")) == null ? void 0 : _a.id) || 0);
    const targetUserId = common_vendor.ref(null);
    const targetName = common_vendor.ref("陪诊师");
    const targetAvatar = common_vendor.ref("/static/doctor-avatar.png");
    const messages = common_vendor.ref([]);
    const inputText = common_vendor.ref("");
    const scrollTop = common_vendor.ref(0);
    const scrollIntoView = common_vendor.ref("");
    const loadingMore = common_vendor.ref(false);
    const hasMoreHistory = common_vendor.ref(true);
    const currentPage = common_vendor.ref(1);
    const isVoiceMode = common_vendor.ref(false);
    const showPanel = common_vendor.ref(false);
    const panelType = common_vendor.ref("");
    const keyboardHeight = common_vendor.ref(0);
    const recording = common_vendor.ref(false);
    const recorderManager = common_vendor.index.getRecorderManager();
    const innerAudioContext = common_vendor.index.createInnerAudioContext();
    const processedMessages = /* @__PURE__ */ new Set();
    const emojiList = ["😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉", "😊", "😋", "😎", "😍", "😘", "🥰", "😗", "😙", "😚", "🙂", "🤗", "🤩", "🤔", "🤨", "😐", "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫", "😴", "😌", "😛", "😜", "😝", "🤤", "😒", "😓", "😔", "😕", "🙃", "🤑", "😲", "☹️", "🙁", "😖", "😞", "😟", "😤", "😢", "😭", "😦", "😧", "😨", "😩", "🤯", "😬", "😰", "😱", "🥵", "🥶", "😳", "🤪", "😵", "😡", "😠", "🤬", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "😇", "🤠", "🤡", "🥳", "🥴", "🥺", "🤥", "🤫", "🤭", "🧐", "🤓", "😈", "👿"];
    const headerSubtitle = common_vendor.computed(() => "患者 · 在线沟通中");
    common_vendor.onLoad((options) => {
      common_vendor.index.stopPullDownRefresh();
      if (!options.userId && !options.attendantId) {
        common_vendor.index.navigateBack();
        return;
      }
      targetUserId.value = parseInt(options.userId || options.attendantId);
      targetName.value = options.name || "陪诊师";
      if (options.avatar)
        targetAvatar.value = getImageUrl(options.avatar);
      utils_chatWebsocket.connectChatSocket();
      loadHistory();
      common_vendor.index.onKeyboardHeightChange((res) => {
        if (res.height > 0) {
          keyboardHeight.value = res.height;
          showPanel.value = false;
          scrollToBottom();
        } else {
          keyboardHeight.value = 0;
        }
      });
      recorderManager.onStop((res) => {
        if (recording.value) {
          sendVoice(res.tempFilePath);
          recording.value = false;
        }
      });
    });
    common_vendor.onMounted(() => utils_chatWebsocket.addChatListener(handleNewMessage));
    common_vendor.onUnmounted(() => utils_chatWebsocket.removeChatListener(handleNewMessage));
    const shouldShowTime = (index) => {
      if (index === 0)
        return true;
      const prevTime = new Date(messages.value[index - 1].createTime).getTime();
      const currTime = new Date(messages.value[index].createTime).getTime();
      return currTime - prevTime > 5 * 60 * 1e3;
    };
    const loadHistory = async () => {
      try {
        const res = await utils_api.get(`/api/chat/history?targetUserId=${targetUserId.value}&page=1&pageSize=${pageSize}`);
        if (res.code === 200) {
          messages.value = res.data;
          hasMoreHistory.value = res.data.length === pageSize;
          setTimeout(() => scrollToBottom(), 100);
        }
      } catch (e) {
      }
    };
    const loadMoreHistory = async () => {
      if (loadingMore.value || !hasMoreHistory.value)
        return;
      loadingMore.value = true;
      try {
        currentPage.value++;
        const res = await utils_api.get(`/api/chat/history?targetUserId=${targetUserId.value}&page=${currentPage.value}&pageSize=${pageSize}`);
        if (res.code === 200 && res.data.length > 0) {
          messages.value = [...res.data.reverse(), ...messages.value];
          hasMoreHistory.value = res.data.length === pageSize;
        } else {
          hasMoreHistory.value = false;
        }
      } catch (e) {
        currentPage.value--;
      } finally {
        loadingMore.value = false;
      }
    };
    const handleNewMessage = (msg) => {
      common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:271", "处理新消息:", msg);
      const msgKey = `${msg.senderId}-${msg.receiverId}-${msg.createTime}-${msg.content}`;
      if (processedMessages.has(msgKey)) {
        common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:276", "消息已处理，跳过:", msgKey);
        return;
      }
      processedMessages.add(msgKey);
      const isNormalChatMsg = [1, 2, 3, 4].includes(Number(msg.msgType)) && msg.content !== "READ_RECEIPT" && msg.type !== "READ_RECEIPT" && msg.type !== "MESSAGE_STATUS_UPDATE";
      if (isNormalChatMsg && (msg.senderId == targetUserId.value || msg.receiverId == targetUserId.value)) {
        if (msg.senderId == targetUserId.value) {
          const newMsg = { ...msg };
          if (!newMsg.senderAvatar || newMsg.senderAvatar === "/static/user-placeholder.png") {
            newMsg.senderAvatar = targetAvatar.value || "/static/user-placeholder.png";
            common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:296", "设置发送者头像:", newMsg.senderAvatar);
          }
          if (!messages.value.some((m) => m.id == newMsg.id)) {
            messages.value.push(newMsg);
            scrollToBottom();
            markAsRead();
          }
        } else if (msg.receiverId == targetUserId.value) {
          if (!messages.value.some((m) => m.id == msg.id)) {
            messages.value.push(msg);
            scrollToBottom();
          }
        }
      }
      if ((msg.type === "READ_RECEIPT" || msg.content === "READ_RECEIPT") && msg.msgType == 3) {
        common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:316", "收到已读回执，更新消息状态");
        const updatedMessages = messages.value.map((m) => {
          if (m.senderId == currentUserId.value && m.isRead !== 1) {
            common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:320", "标记消息为已读:", m.id);
            return { ...m, isRead: 1 };
          }
          return m;
        });
        messages.value = updatedMessages;
        common_vendor.nextTick$1(() => {
          common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:331", "已读状态更新完成");
        });
        return;
      }
      if (msg.type === "MESSAGE_STATUS_UPDATE") {
        common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:338", "收到消息状态更新:", msg);
        const messageId = msg.messageId || msg.id;
        if (messageId) {
          messages.value = messages.value.map((m) => {
            if (m.id == messageId) {
              return { ...m, ...msg.updates };
            }
            return m;
          });
        }
        return;
      }
    };
    const sendMessage = async (content, type) => {
      const userInfo = common_vendor.index.getStorageSync("userInfo");
      const tempMsg = {
        id: "temp-" + Date.now(),
        senderId: currentUserId.value,
        receiverId: targetUserId.value,
        content,
        msgType: type,
        senderName: (userInfo == null ? void 0 : userInfo.name) || "我",
        senderAvatar: userInfo == null ? void 0 : userInfo.avatar,
        createTime: /* @__PURE__ */ new Date(),
        status: "sending",
        isRead: 0
      };
      messages.value.push(tempMsg);
      scrollToBottom();
      const tempIndex = messages.value.length - 1;
      try {
        const res = await utils_api.post("/api/chat/send", { receiverId: targetUserId.value, content, msgType: type });
        if (res.code === 200)
          messages.value[tempIndex] = { ...res.data, status: "sent" };
        else
          throw new Error("Failed");
      } catch (e) {
        messages.value[tempIndex].status = "failed";
      }
    };
    const sendText = () => {
      if (!inputText.value.trim())
        return;
      sendMessage(inputText.value, 1);
      inputText.value = "";
    };
    const chooseImage = (sourceType) => {
      common_vendor.index.chooseImage({
        count: 1,
        sourceType: sourceType ? [sourceType] : ["album", "camera"],
        success: async (res) => {
          const path = res.tempFilePaths[0];
          try {
            const uploadRes = await utils_api.upload("/common/upload", path);
            if (uploadRes.code === 200) {
              sendMessage(uploadRes.url, 2);
            }
          } catch (e) {
            common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:388", e);
          }
        }
      });
    };
    const sendVoice = async (path) => {
      try {
        const uploadRes = await utils_api.upload("/common/upload", path);
        if (uploadRes.code === 200) {
          sendMessage(uploadRes.url, 3);
        }
      } catch (e) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:399", e);
      }
    };
    const chooseLocation = () => {
      common_vendor.index.chooseLocation({
        success: (res) => {
          const locationData = JSON.stringify({
            name: res.name,
            address: res.address,
            latitude: res.latitude,
            longitude: res.longitude
          });
          sendMessage(locationData, 4);
        }
      });
    };
    const sendEmergency = () => {
      sendMessage("【紧急求助】请立即联系我！", 1);
    };
    const videoCall = () => {
      common_vendor.index.showToast({ title: "视频通话功能开发中", icon: "none" });
    };
    const voiceCall = () => {
      common_vendor.index.showToast({ title: "语音通话功能开发中", icon: "none" });
    };
    const switchVoiceMode = () => {
      isVoiceMode.value = !isVoiceMode.value;
      if (isVoiceMode.value) {
        showPanel.value = false;
        common_vendor.index.hideKeyboard();
      } else {
        common_vendor.nextTick$1(() => {
        });
      }
    };
    const toggleEmoji = () => {
      if (panelType.value === "emoji" && showPanel.value) {
        showPanel.value = false;
      } else {
        panelType.value = "emoji";
        showPanel.value = true;
        isVoiceMode.value = false;
        common_vendor.index.hideKeyboard();
        scrollToBottom();
      }
    };
    const toggleMore = () => {
      if (panelType.value === "more" && showPanel.value) {
        showPanel.value = false;
      } else {
        panelType.value = "more";
        showPanel.value = true;
        isVoiceMode.value = false;
        common_vendor.index.hideKeyboard();
        scrollToBottom();
      }
    };
    const closePanel = () => {
      showPanel.value = false;
      common_vendor.index.hideKeyboard();
    };
    const onInputFocus = (e) => {
      showPanel.value = false;
      keyboardHeight.value = e.detail.height;
      scrollToBottom();
    };
    const onInputBlur = () => {
      keyboardHeight.value = 0;
    };
    const addEmoji = (emoji) => {
      inputText.value += emoji;
    };
    const startRecord = () => {
      recording.value = true;
      recorderManager.start();
    };
    const stopRecord = () => {
      recorderManager.stop();
    };
    const cancelRecord = () => {
      recording.value = false;
      recorderManager.stop();
    };
    const playVoice = (url) => {
      innerAudioContext.src = getImageUrl(url);
      innerAudioContext.play();
    };
    const openLocation = (content) => {
      try {
        const loc = JSON.parse(content);
        common_vendor.index.openLocation({
          latitude: loc.latitude,
          longitude: loc.longitude,
          name: loc.name,
          address: loc.address
        });
      } catch (e) {
      }
    };
    const parseLocation = (content) => {
      try {
        return JSON.parse(content);
      } catch (e) {
        return {};
      }
    };
    const markAsRead = () => {
      utils_api.post(`/api/chat/read?senderId=${targetUserId.value}`);
    };
    const navigateBack = () => {
      common_vendor.index.$emit("chat:return", { targetUserId: targetUserId.value });
      common_vendor.index.navigateBack();
    };
    const scrollToBottom = () => {
      common_vendor.nextTick$1(() => {
        scrollIntoView.value = "msg-" + (messages.value.length - 1);
      });
    };
    const getAvatar = (msg) => {
      var _a2;
      if (msg.senderId === currentUserId.value) {
        const currentUserAvatar = (_a2 = common_vendor.index.getStorageSync("userInfo")) == null ? void 0 : _a2.avatar;
        const avatarUrl = currentUserAvatar || "/static/user-placeholder.png";
        common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:534", "当前陪诊师头像:", avatarUrl);
        return getImageUrl(avatarUrl);
      }
      if (msg.senderAvatar && msg.senderAvatar !== "/static/user-placeholder.png" && msg.senderAvatar !== "/static/doctor-avatar.png") {
        common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:540", "使用消息中的用户头像:", msg.senderAvatar);
        return getImageUrl(msg.senderAvatar);
      }
      if (targetAvatar.value && targetAvatar.value !== "/static/user-placeholder.png" && targetAvatar.value !== "/static/doctor-avatar.png") {
        common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:546", "使用预加载用户头像:", targetAvatar.value);
        return targetAvatar.value;
      }
      common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:551", "使用默认用户头像");
      return "/static/user-placeholder.png";
    };
    const getImageUrl = (url) => {
      if (!url)
        return "/static/user-placeholder.png";
      if (url.startsWith("http") || url.startsWith("wxfile"))
        return url;
      const baseUrl = utils_api.config.baseURL.endsWith("/") ? utils_api.config.baseURL.slice(0, -1) : utils_api.config.baseURL;
      return baseUrl + (url.startsWith("/") ? url : "/" + url);
    };
    const previewImage = (url) => common_vendor.index.previewImage({ urls: [url], current: url });
    const handleAvatarError = (e) => {
      common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:561", "头像加载失败:", e.detail.errMsg);
    };
    const formatTimeCenter = (time) => {
      if (!time)
        return "";
      const d = new Date(time);
      const now = /* @__PURE__ */ new Date();
      if (d.toDateString() === now.toDateString())
        return `${d.getHours().toString().padStart(2, "0")}:${d.getMinutes().toString().padStart(2, "0")}`;
      return `${d.getMonth() + 1}月${d.getDate()}日 ${d.getHours().toString().padStart(2, "0")}:${d.getMinutes().toString().padStart(2, "0")}`;
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(navigateBack),
        b: common_vendor.t(targetName.value),
        c: common_vendor.t(headerSubtitle.value),
        d: loadingMore.value
      }, loadingMore.value ? {} : {}, {
        e: common_vendor.f(messages.value, (msg, index, i0) => {
          return common_vendor.e({
            a: shouldShowTime(index)
          }, shouldShowTime(index) ? {
            b: common_vendor.t(formatTimeCenter(msg.createTime))
          } : {}, {
            c: getAvatar(msg),
            d: common_vendor.o(handleAvatarError, msg.id || index),
            e: msg.senderId !== currentUserId.value
          }, msg.senderId !== currentUserId.value ? {
            f: common_vendor.t(msg.senderName || targetName.value)
          } : {}, {
            g: msg.senderId === currentUserId.value && msg.status !== "sending" && msg.status !== "failed"
          }, msg.senderId === currentUserId.value && msg.status !== "sending" && msg.status !== "failed" ? {
            h: common_vendor.t(msg.isRead ? "已读" : "未读"),
            i: msg.isRead ? 1 : ""
          } : {}, {
            j: msg.senderId === currentUserId.value && msg.status === "sending"
          }, msg.senderId === currentUserId.value && msg.status === "sending" ? {} : {}, {
            k: msg.senderId === currentUserId.value && msg.status === "failed"
          }, msg.senderId === currentUserId.value && msg.status === "failed" ? {} : {}, {
            l: msg.msgType === 1
          }, msg.msgType === 1 ? {
            m: common_vendor.t(msg.content)
          } : msg.msgType === 2 ? {
            o: getImageUrl(msg.content),
            p: common_vendor.o(($event) => previewImage(getImageUrl(msg.content)), msg.id || index)
          } : msg.msgType === 3 ? {
            r: common_assets._imports_0$5,
            s: common_vendor.o(($event) => playVoice(msg.content), msg.id || index)
          } : msg.msgType === 4 ? {
            v: common_assets._imports_1$3,
            w: common_vendor.t(parseLocation(msg.content).name || "位置信息"),
            x: common_vendor.o(($event) => openLocation(msg.content), msg.id || index)
          } : {}, {
            n: msg.msgType === 2,
            q: msg.msgType === 3,
            t: msg.msgType === 4,
            y: msg.msgType === 3 ? 1 : "",
            z: msg.senderId === currentUserId.value ? 1 : "",
            A: msg.id || index,
            B: "msg-" + index
          });
        }),
        f: (showPanel.value ? 500 : 0) + 140 + "rpx",
        g: scrollTop.value,
        h: scrollIntoView.value,
        i: common_vendor.o(loadMoreHistory),
        j: common_vendor.o(closePanel),
        k: isVoiceMode.value ? "/static/icons/keyboard.png" : "/static/icons/voice.png",
        l: common_vendor.o(switchVoiceMode),
        m: isVoiceMode.value
      }, isVoiceMode.value ? {
        n: common_vendor.t(recording.value ? "松开 结束" : "按住 说话"),
        o: recording.value ? 1 : "",
        p: common_vendor.o(startRecord),
        q: common_vendor.o(stopRecord),
        r: common_vendor.o(cancelRecord)
      } : {
        s: common_vendor.o(sendText),
        t: common_vendor.o(onInputFocus),
        v: common_vendor.o(onInputBlur),
        w: inputText.value,
        x: common_vendor.o(($event) => inputText.value = $event.detail.value)
      }, {
        y: common_assets._imports_2$4,
        z: common_vendor.o(toggleEmoji),
        A: inputText.value.trim() && !isVoiceMode.value
      }, inputText.value.trim() && !isVoiceMode.value ? {
        B: common_vendor.o(sendText)
      } : {
        C: common_assets._imports_3$3,
        D: common_vendor.o(toggleMore)
      }, {
        E: showPanel.value
      }, showPanel.value ? common_vendor.e({
        F: panelType.value === "emoji"
      }, panelType.value === "emoji" ? {
        G: common_vendor.f(emojiList, (emoji, index, i0) => {
          return {
            a: common_vendor.t(emoji),
            b: index,
            c: common_vendor.o(($event) => addEmoji(emoji), index)
          };
        })
      } : {}, {
        H: panelType.value === "more"
      }, panelType.value === "more" ? {
        I: common_assets._imports_4$2,
        J: common_vendor.o(($event) => chooseImage("album")),
        K: common_assets._imports_5$1,
        L: common_vendor.o(($event) => chooseImage("camera")),
        M: common_assets._imports_1$3,
        N: common_vendor.o(chooseLocation),
        O: common_assets._imports_6$1,
        P: common_vendor.o(sendEmergency),
        Q: common_assets._imports_7$1,
        R: common_vendor.o(videoCall),
        S: common_assets._imports_8$1,
        T: common_vendor.o(voiceCall)
      } : {}) : {}, {
        U: keyboardHeight.value + "px"
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-4fc73bec"]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/chat/chat.js.map
