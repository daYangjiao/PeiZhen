"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const _sfc_main = {
  __name: "chat",
  setup(__props) {
    const chatUser = common_vendor.ref({
      id: "",
      name: "",
      avatar: "/static/avatar1.png"
    });
    const myAvatar = common_vendor.ref("/static/doctor-avatar.png");
    const messageList = common_vendor.ref([]);
    const chatId = common_vendor.ref("");
    const syncTimer = common_vendor.ref(null);
    const lastSyncTime = common_vendor.ref(0);
    const isOnline = common_vendor.ref(true);
    const isConnecting = common_vendor.ref(false);
    const retryCount = common_vendor.ref(0);
    const maxRetries = common_vendor.ref(3);
    const showMenu = common_vendor.ref(false);
    const menuPosition = common_vendor.ref({ x: 0, y: 0 });
    const selectedMessage = common_vendor.ref(null);
    const isLoadingMore = common_vendor.ref(false);
    const hasMoreMessages = common_vendor.ref(true);
    const pageSize = common_vendor.ref(20);
    const currentPage = common_vendor.ref(1);
    const showSearchBar = common_vendor.ref(false);
    const searchKeyword = common_vendor.ref("");
    const searchResults = common_vendor.ref([]);
    const showEmojiPanel = common_vendor.ref(false);
    const showQuickReply = common_vendor.ref(false);
    const isTyping = common_vendor.ref(false);
    const typingTimer = common_vendor.ref(null);
    const inputText = common_vendor.ref("");
    const inputFocus = common_vendor.ref(false);
    const isVoiceInput = common_vendor.ref(false);
    const isRecording = common_vendor.ref(false);
    const scrollTop = common_vendor.ref(0);
    const recorderManager = common_vendor.ref(null);
    const innerAudioContext = common_vendor.ref(null);
    common_vendor.onMounted(() => {
      const pages = getCurrentPages();
      const currentPage2 = pages[pages.length - 1];
      const options = currentPage2.options;
      if (options.userId) {
        chatUser.value.id = options.userId;
      }
      if (options.name) {
        chatUser.value.name = decodeURIComponent(options.name);
      }
      common_vendor.index.onNetworkStatusChange((res) => {
        isOnline.value = res.isConnected;
        if (res.isConnected) {
          common_vendor.index.showToast({
            title: "网络已连接",
            icon: "success",
            duration: 1500
          });
          startMessageSync();
          retryFailedMessages();
        } else {
          common_vendor.index.showToast({
            title: "网络连接断开",
            icon: "none",
            duration: 2e3
          });
          stopMessageSync();
        }
      });
      common_vendor.index.getNetworkType({
        success: (res) => {
          isOnline.value = res.networkType !== "none";
        }
      });
      initChat();
      startMessageSync();
      common_vendor.nextTick$1(() => {
        scrollToBottom();
      });
    });
    common_vendor.onUnmounted(() => {
      stopMessageSync();
    });
    const goBack = () => {
      common_vendor.index.navigateBack();
    };
    const makeCall = () => {
      common_vendor.index.makePhoneCall({
        phoneNumber: "138****8888"
      });
    };
    const showTimeDivider = (index) => {
      if (index === 0)
        return true;
      const current = messageList.value[index];
      const previous = messageList.value[index - 1];
      return current.timestamp - previous.timestamp > 3e5;
    };
    const formatTime = (timestamp) => {
      const date = new Date(timestamp);
      const now = /* @__PURE__ */ new Date();
      if (date.toDateString() === now.toDateString()) {
        return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
      } else {
        return date.toLocaleString("zh-CN", {
          month: "2-digit",
          day: "2-digit",
          hour: "2-digit",
          minute: "2-digit"
        });
      }
    };
    const sendTextMessage = () => {
      if (!inputText.value.trim())
        return;
      const message = {
        id: Date.now(),
        type: "text",
        content: inputText.value.trim(),
        isMine: true,
        timestamp: Date.now(),
        status: "sending"
        // 发送状态
      };
      messageList.value.push(message);
      inputText.value = "";
      saveMessagesToLocal();
      sendMessageToServer(message);
      common_vendor.nextTick$1(() => {
        scrollToBottom();
      });
    };
    const sendMessageToServer = async (message) => {
      await sendMessageToServerEnhanced(message);
    };
    const toggleVoiceInput = () => {
      isVoiceInput.value = !isVoiceInput.value;
      inputFocus.value = !isVoiceInput.value;
    };
    const startRecord = () => {
      isRecording.value = true;
      if (!recorderManager.value) {
        recorderManager.value = common_vendor.index.getRecorderManager();
        recorderManager.value.onStart(() => {
          common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:459", "录音开始");
        });
        recorderManager.value.onStop((res) => {
          common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:463", "录音结束", res);
          if (res.duration > 1e3) {
            sendVoiceMessage(res.tempFilePath, Math.floor(res.duration / 1e3));
          } else {
            common_vendor.index.showToast({
              title: "录音时间太短",
              icon: "none"
            });
          }
        });
        recorderManager.value.onError((err) => {
          common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:475", "录音错误", err);
          common_vendor.index.showToast({
            title: "录音失败",
            icon: "none"
          });
          isRecording.value = false;
        });
      }
      recorderManager.value.start({
        duration: 6e4,
        // 最长60秒
        sampleRate: 16e3,
        numberOfChannels: 1,
        encodeBitRate: 96e3,
        format: "mp3"
      });
    };
    const stopRecord = () => {
      isRecording.value = false;
      if (recorderManager.value) {
        recorderManager.value.stop();
      }
    };
    const cancelRecord = () => {
      isRecording.value = false;
      if (recorderManager.value) {
        recorderManager.value.stop();
      }
      common_vendor.index.showToast({
        title: "录音取消",
        icon: "none"
      });
    };
    const chooseImage = () => {
      common_vendor.index.chooseImage({
        count: 1,
        sourceType: ["album", "camera"],
        sizeType: ["compressed"],
        // 压缩图片
        success: (res) => {
          const tempFilePath = res.tempFilePaths[0];
          common_vendor.index.showLoading({
            title: "图片处理中..."
          });
          common_vendor.index.compressImage({
            src: tempFilePath,
            quality: 80,
            // 压缩质量
            success: (compressRes) => {
              common_vendor.index.hideLoading();
              const message = {
                id: Date.now(),
                type: "image",
                content: compressRes.tempFilePath,
                isMine: true,
                timestamp: Date.now(),
                status: "sending"
                // 发送状态
              };
              messageList.value.push(message);
              saveMessagesToLocal();
              common_vendor.nextTick$1(() => {
                scrollToBottom();
              });
              uploadImage(message, compressRes.tempFilePath);
            },
            fail: (err) => {
              common_vendor.index.hideLoading();
              common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:558", "图片压缩失败", err);
              const message = {
                id: Date.now(),
                type: "image",
                content: tempFilePath,
                isMine: true,
                timestamp: Date.now(),
                status: "sending"
              };
              messageList.value.push(message);
              saveMessagesToLocal();
              common_vendor.nextTick$1(() => {
                scrollToBottom();
              });
              uploadImage(message);
            }
          });
        },
        fail: (err) => {
          common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:584", "选择图片失败", err);
          common_vendor.index.showToast({
            title: "选择图片失败",
            icon: "none"
          });
        }
      });
    };
    const uploadImage = (message, filePath) => {
      setTimeout(() => {
        const index = messageList.value.findIndex((m) => m.id === message.id);
        if (index !== -1) {
          messageList.value[index].status = "sent";
        }
        common_vendor.index.showToast({
          title: "图片发送成功",
          icon: "success"
        });
      }, 2e3);
    };
    const sendLocation = () => {
      common_vendor.index.chooseLocation({
        success: (res) => {
          const message = {
            id: Date.now(),
            type: "location",
            locationName: res.name,
            locationAddress: res.address,
            latitude: res.latitude,
            longitude: res.longitude,
            isMine: true,
            timestamp: Date.now(),
            status: "sending"
          };
          messageList.value.push(message);
          saveMessagesToLocal();
          sendMessageToServer(message);
          common_vendor.nextTick$1(() => {
            scrollToBottom();
          });
        }
      });
    };
    const getStatusText = (status) => {
      switch (status) {
        case "sending":
          return "发送中";
        case "sent":
          return "已发送";
        case "read":
          return "已读";
        case "failed":
          return "发送失败";
        default:
          return "";
      }
    };
    const handleStatusClick = (message) => {
      if (message.status === "failed") {
        retryMessage(message);
      }
    };
    const retryMessage = async (message) => {
      try {
        const index = messageList.value.findIndex((m) => m.id === message.id);
        if (index !== -1) {
          messageList.value[index].status = "sending";
          saveMessagesToLocal();
        }
        await sendMessageToServerEnhanced(message);
        common_vendor.index.showToast({
          title: "重新发送中...",
          icon: "loading"
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:684", "重试发送失败:", error);
      }
    };
    const showMessageMenu = (message, event) => {
      selectedMessage.value = message;
      const touch = event.touches ? event.touches[0] : event;
      menuPosition.value = {
        x: touch.clientX || touch.pageX || 0,
        y: touch.clientY || touch.pageY || 0
      };
      const screenWidth = common_vendor.index.getSystemInfoSync().windowWidth;
      const screenHeight = common_vendor.index.getSystemInfoSync().windowHeight;
      const menuWidth = 200;
      const menuHeight = 150;
      if (menuPosition.value.x + menuWidth > screenWidth) {
        menuPosition.value.x = screenWidth - menuWidth - 20;
      }
      if (menuPosition.value.y + menuHeight > screenHeight) {
        menuPosition.value.y = menuPosition.value.y - menuHeight - 20;
      }
      showMenu.value = true;
    };
    const hideMessageMenu = () => {
      showMenu.value = false;
      selectedMessage.value = null;
    };
    const copyMessage = () => {
      if (selectedMessage.value && selectedMessage.value.type === "text") {
        common_vendor.index.setClipboardData({
          data: selectedMessage.value.content,
          success: () => {
            common_vendor.index.showToast({
              title: "复制成功",
              icon: "success"
            });
          }
        });
      }
      hideMessageMenu();
    };
    const canRecall = (message) => {
      const now = Date.now();
      const messageTime = message.timestamp;
      return now - messageTime < 2 * 60 * 1e3;
    };
    const recallMessage = async () => {
      if (!selectedMessage.value)
        return;
      try {
        const index = messageList.value.findIndex((m) => m.id === selectedMessage.value.id);
        if (index !== -1) {
          messageList.value[index] = {
            ...messageList.value[index],
            type: "system",
            content: "你撤回了一条消息",
            isRecalled: true
          };
          saveMessagesToLocal();
        }
        common_vendor.index.showToast({
          title: "撤回成功",
          icon: "success"
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:775", "撤回失败:", error);
        common_vendor.index.showToast({
          title: "撤回失败",
          icon: "none"
        });
      }
      hideMessageMenu();
    };
    const deleteMessage = () => {
      if (!selectedMessage.value)
        return;
      common_vendor.index.showModal({
        title: "确认删除",
        content: "确定要删除这条消息吗？",
        success: (res) => {
          if (res.confirm) {
            const index = messageList.value.findIndex((m) => m.id === selectedMessage.value.id);
            if (index !== -1) {
              messageList.value.splice(index, 1);
              saveMessagesToLocal();
              common_vendor.index.showToast({
                title: "删除成功",
                icon: "success"
              });
            }
          }
        }
      });
      hideMessageMenu();
    };
    const previewImage = (url) => {
      common_vendor.index.previewImage({
        urls: [url]
      });
    };
    const sendVoiceMessage = (filePath, duration) => {
      const message = {
        id: Date.now(),
        type: "voice",
        content: filePath,
        duration,
        isMine: true,
        timestamp: Date.now(),
        status: "sending"
      };
      messageList.value.push(message);
      saveMessagesToLocal();
      sendMessageToServer(message);
      common_vendor.nextTick$1(() => {
        scrollToBottom();
      });
    };
    const playVoice = (message) => {
      if (!innerAudioContext.value) {
        innerAudioContext.value = common_vendor.index.createInnerAudioContext();
        innerAudioContext.value.onPlay(() => {
          common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:850", "开始播放语音");
        });
        innerAudioContext.value.onStop(() => {
          common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:854", "停止播放语音");
        });
        innerAudioContext.value.onEnded(() => {
          common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:858", "语音播放结束");
        });
        innerAudioContext.value.onError((err) => {
          common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:862", "语音播放错误", err);
          common_vendor.index.showToast({
            title: "播放失败",
            icon: "none"
          });
        });
      }
      innerAudioContext.value.src = message.content;
      innerAudioContext.value.play();
      common_vendor.index.showToast({
        title: "正在播放语音",
        icon: "none"
      });
    };
    const openLocation = (message) => {
      common_vendor.index.openLocation({
        latitude: message.latitude,
        longitude: message.longitude,
        name: message.locationName,
        address: message.locationAddress
      });
    };
    const scrollToBottom = () => {
      scrollTop.value = 999999;
    };
    const initChat = async () => {
      try {
        chatId.value = `chat_${chatUser.value.id}_${Date.now()}`;
        const recentMessages = await loadHistoryMessages(1);
        if (recentMessages && recentMessages.length > 0) {
          messageList.value = recentMessages;
          currentPage.value = 1;
          lastSyncTime.value = Math.max(...recentMessages.map((m) => m.timestamp));
          if (recentMessages.length < pageSize.value) {
            hasMoreMessages.value = false;
          }
        } else {
          const welcomeMessage = {
            id: Date.now(),
            type: "text",
            content: "您好，我是您的陪诊师，请问您现在在哪里？",
            isMine: true,
            timestamp: Date.now(),
            status: "read"
          };
          messageList.value = [welcomeMessage];
          saveMessagesToLocal();
          hasMoreMessages.value = false;
        }
        await syncMessagesFromServer();
        common_vendor.nextTick$1(() => {
          scrollToBottom();
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:933", "初始化聊天失败:", error);
        common_vendor.index.showToast({
          title: "聊天初始化失败",
          icon: "none"
        });
      }
    };
    const startMessageSync = () => {
      syncTimer.value = setInterval(async () => {
        await syncMessagesFromServer();
      }, 5e3);
    };
    const stopMessageSync = () => {
      if (syncTimer.value) {
        clearInterval(syncTimer.value);
        syncTimer.value = null;
      }
    };
    const syncMessagesFromServer = async () => {
      try {
        if (!isOnline.value) {
          common_vendor.index.__f__("log", "at subpkg/chat/chat.vue:962", "网络未连接，跳过同步");
          return;
        }
        isConnecting.value = true;
        const mockResponse = {
          data: {
            success: true,
            messages: [],
            // 新消息列表
            updatedMessages: []
            // 更新的消息状态
          }
        };
        const { messages: newMessages, updatedMessages } = mockResponse.data;
        if (newMessages && newMessages.length > 0) {
          newMessages.forEach((message) => {
            const existingIndex = messageList.value.findIndex((m) => m.id === message.id);
            if (existingIndex === -1) {
              messageList.value.push(message);
              lastSyncTime.value = Math.max(lastSyncTime.value, message.timestamp);
            }
          });
          messageList.value.sort((a, b) => a.timestamp - b.timestamp);
          common_vendor.nextTick$1(() => {
            scrollToBottom();
          });
        }
        if (updatedMessages && updatedMessages.length > 0) {
          updatedMessages.forEach((update) => {
            const messageIndex = messageList.value.findIndex((m) => m.id === update.id);
            if (messageIndex !== -1) {
              messageList.value[messageIndex] = { ...messageList.value[messageIndex], ...update };
            }
          });
        }
        saveMessagesToLocal();
        retryCount.value = 0;
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:1027", "同步消息失败:", error);
        retryCount.value++;
        if (retryCount.value < maxRetries.value) {
          setTimeout(() => {
            syncMessagesFromServer();
          }, 5e3 * retryCount.value);
        } else {
          common_vendor.index.showToast({
            title: "同步失败，请检查网络",
            icon: "none",
            duration: 2e3
          });
        }
      } finally {
        isConnecting.value = false;
      }
    };
    const saveMessagesToLocal = () => {
      try {
        common_vendor.index.setStorageSync(`messages_${chatUser.value.id}`, messageList.value);
        common_vendor.index.setStorageSync(`lastSyncTime_${chatUser.value.id}`, lastSyncTime.value);
        saveToAllMessages();
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:1059", "保存消息到本地失败:", error);
      }
    };
    const saveToAllMessages = () => {
      try {
        const allMessages = common_vendor.index.getStorageSync(`all_messages_${chatUser.value.id}`) || [];
        const mergedMessages = [...allMessages];
        messageList.value.forEach((message) => {
          const existingIndex = mergedMessages.findIndex((m) => m.id === message.id);
          if (existingIndex === -1) {
            mergedMessages.push(message);
          } else {
            mergedMessages[existingIndex] = message;
          }
        });
        mergedMessages.sort((a, b) => a.timestamp - b.timestamp);
        const maxHistoryMessages = 1e3;
        if (mergedMessages.length > maxHistoryMessages) {
          mergedMessages.splice(0, mergedMessages.length - maxHistoryMessages);
        }
        common_vendor.index.setStorageSync(`all_messages_${chatUser.value.id}`, mergedMessages);
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:1095", "保存到全部历史消息失败:", error);
      }
    };
    const retryFailedMessages = async () => {
      const failedMessages = messageList.value.filter((m) => m.isMine && m.status === "failed");
      for (const message of failedMessages) {
        await sendMessageToServerEnhanced(message);
      }
    };
    const loadMoreMessages = async () => {
      if (isLoadingMore.value || !hasMoreMessages.value) {
        return;
      }
      isLoadingMore.value = true;
      try {
        const moreMessages = await loadHistoryMessages(currentPage.value + 1);
        if (moreMessages && moreMessages.length > 0) {
          messageList.value.unshift(...moreMessages);
          currentPage.value++;
          if (moreMessages.length < pageSize.value) {
            hasMoreMessages.value = false;
          }
          common_vendor.index.showToast({
            title: `加载了 ${moreMessages.length} 条历史消息`,
            icon: "success",
            duration: 1500
          });
        } else {
          hasMoreMessages.value = false;
          common_vendor.index.showToast({
            title: "没有更多历史消息了",
            icon: "none",
            duration: 1500
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:1143", "加载历史消息失败:", error);
        common_vendor.index.showToast({
          title: "加载失败，请重试",
          icon: "none"
        });
      } finally {
        isLoadingMore.value = false;
      }
    };
    const onRefresh = async () => {
      await loadMoreMessages();
    };
    const loadHistoryMessages = async (page) => {
      try {
        const allMessages = common_vendor.index.getStorageSync(`all_messages_${chatUser.value.id}`) || [];
        allMessages.sort((a, b) => b.timestamp - a.timestamp);
        const startIndex = (page - 1) * pageSize.value;
        const endIndex = startIndex + pageSize.value;
        const pageMessages = allMessages.slice(startIndex, endIndex);
        return pageMessages.reverse();
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:1177", "从本地存储加载历史消息失败:", error);
        return [];
      }
    };
    const clearChatHistory = () => {
      common_vendor.index.showModal({
        title: "确认清空",
        content: "确定要清空所有聊天记录吗？此操作不可恢复。",
        success: (res) => {
          if (res.confirm) {
            messageList.value = [];
            try {
              common_vendor.index.removeStorageSync(`messages_${chatUser.value.id}`);
              common_vendor.index.removeStorageSync(`all_messages_${chatUser.value.id}`);
              common_vendor.index.removeStorageSync(`lastSyncTime_${chatUser.value.id}`);
              lastSyncTime.value = 0;
              currentPage.value = 1;
              hasMoreMessages.value = true;
              common_vendor.index.showToast({
                title: "聊天记录已清空",
                icon: "success"
              });
            } catch (error) {
              common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:1208", "清空聊天记录失败:", error);
              common_vendor.index.showToast({
                title: "清空失败",
                icon: "none"
              });
            }
          }
        }
      });
    };
    const toggleSearch = () => {
      showSearchBar.value = !showSearchBar.value;
      if (!showSearchBar.value) {
        searchKeyword.value = "";
        searchResults.value = [];
      }
    };
    const closeSearch = () => {
      showSearchBar.value = false;
      searchKeyword.value = "";
      searchResults.value = [];
    };
    const onSearchInput = () => {
      if (searchKeyword.value.trim()) {
        searchMessages();
      } else {
        searchResults.value = [];
      }
    };
    const searchMessages = () => {
      if (!searchKeyword.value.trim()) {
        return;
      }
      try {
        const allMessages = common_vendor.index.getStorageSync(`all_messages_${chatUser.value.id}`) || [];
        const results = allMessages.filter((message) => {
          if (message.type === "text") {
            return message.content.toLowerCase().includes(searchKeyword.value.toLowerCase());
          }
          return false;
        });
        searchResults.value = results;
        if (results.length > 0) {
          common_vendor.index.showToast({
            title: `找到 ${results.length} 条相关消息`,
            icon: "success"
          });
        } else {
          common_vendor.index.showToast({
            title: "未找到相关消息",
            icon: "none"
          });
        }
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:1273", "搜索消息失败:", error);
        common_vendor.index.showToast({
          title: "搜索失败",
          icon: "none"
        });
      }
    };
    const insertEmoji = (emoji) => {
      inputText.value += emoji;
      showEmojiPanel.value = false;
    };
    const sendQuickReply = (text) => {
      inputText.value = text;
      showQuickReply.value = false;
      sendTextMessage();
    };
    const onInputChange = () => {
      isTyping.value = true;
      if (typingTimer.value) {
        clearTimeout(typingTimer.value);
      }
      typingTimer.value = setTimeout(() => {
        isTyping.value = false;
      }, 2e3);
    };
    const exportChatHistory = () => {
      try {
        const allMessages = common_vendor.index.getStorageSync(`all_messages_${chatUser.value.id}`) || [];
        if (allMessages.length === 0) {
          common_vendor.index.showToast({
            title: "暂无聊天记录",
            icon: "none"
          });
          return;
        }
        let exportText = `聊天记录 - ${chatUser.value.name}
`;
        exportText += `导出时间: ${(/* @__PURE__ */ new Date()).toLocaleString()}
`;
        exportText += `消息总数: ${allMessages.length}
`;
        exportText += "\n" + "=".repeat(50) + "\n\n";
        allMessages.forEach((message) => {
          const time = new Date(message.timestamp).toLocaleString();
          const sender = message.isMine ? "我" : chatUser.value.name;
          let content = "";
          switch (message.type) {
            case "text":
              content = message.content;
              break;
            case "image":
              content = "[图片]";
              break;
            case "voice":
              content = `[语音 ${message.duration}秒]`;
              break;
            case "location":
              content = `[位置] ${message.locationName}`;
              break;
            case "system":
              content = `[系统消息] ${message.content}`;
              break;
            default:
              content = "[未知消息类型]";
          }
          exportText += `[${time}] ${sender}: ${content}
`;
        });
        common_vendor.index.setClipboardData({
          data: exportText,
          success: () => {
            common_vendor.index.showToast({
              title: "聊天记录已复制到剪贴板",
              icon: "success",
              duration: 2e3
            });
          },
          fail: () => {
            common_vendor.index.showToast({
              title: "导出失败",
              icon: "none"
            });
          }
        });
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:1375", "导出聊天记录失败:", error);
        common_vendor.index.showToast({
          title: "导出失败",
          icon: "none"
        });
      }
    };
    const sendMessageToServerEnhanced = async (message) => {
      try {
        if (!isOnline.value) {
          throw new Error("网络未连接");
        }
        await new Promise((resolve) => setTimeout(resolve, 1e3));
        const index = messageList.value.findIndex((m) => m.id === message.id);
        if (index !== -1) {
          messageList.value[index].status = "sent";
          messageList.value[index].serverMessageId = `server_${message.id}`;
          saveMessagesToLocal();
          setTimeout(() => {
            if (messageList.value[index]) {
              messageList.value[index].status = "read";
              saveMessagesToLocal();
            }
          }, 3e3);
        }
        return { success: true };
      } catch (error) {
        common_vendor.index.__f__("error", "at subpkg/chat/chat.vue:1425", "发送消息失败:", error);
        const index = messageList.value.findIndex((m) => m.id === message.id);
        if (index !== -1) {
          messageList.value[index].status = "failed";
          saveMessagesToLocal();
        }
        let errorMessage = "发送失败，请重试";
        if (error.message === "网络未连接") {
          errorMessage = "网络未连接，消息将在网络恢复后重试";
        } else if (error.message.includes("timeout")) {
          errorMessage = "网络超时，请检查网络连接";
        }
        common_vendor.index.showToast({
          title: errorMessage,
          icon: "none",
          duration: 2e3
        });
        return { success: false, error };
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_0$4,
        b: common_vendor.o(goBack),
        c: common_vendor.t(chatUser.value.name),
        d: common_vendor.t(isConnecting.value ? "连接中..." : isOnline.value ? "在线" : "离线"),
        e: !isOnline.value ? 1 : "",
        f: isConnecting.value ? 1 : "",
        g: common_assets._imports_1$2,
        h: common_vendor.o(makeCall),
        i: isLoadingMore.value
      }, isLoadingMore.value ? {} : !hasMoreMessages.value && messageList.value.length > pageSize.value ? {} : {}, {
        j: !hasMoreMessages.value && messageList.value.length > pageSize.value,
        k: isConnecting.value
      }, isConnecting.value ? {} : {}, {
        l: !isOnline.value
      }, !isOnline.value ? {} : {}, {
        m: isTyping.value
      }, isTyping.value ? {
        n: common_vendor.t(chatUser.value.name)
      } : {}, {
        o: common_vendor.f(messageList.value, (message, index, i0) => {
          return common_vendor.e({
            a: showTimeDivider(index)
          }, showTimeDivider(index) ? {
            b: common_vendor.t(formatTime(message.timestamp))
          } : {}, {
            c: message.isMine ? myAvatar.value : chatUser.value.avatar,
            d: message.type === "text"
          }, message.type === "text" ? {
            e: common_vendor.t(message.content)
          } : message.type === "image" ? {
            g: message.content,
            h: common_vendor.o(($event) => previewImage(message.content), index)
          } : message.type === "voice" ? {
            j: common_assets._imports_2$2,
            k: common_vendor.t(message.duration),
            l: common_vendor.o(($event) => playVoice(message), index)
          } : message.type === "location" ? {
            n: common_assets._imports_3$3,
            o: common_vendor.t(message.locationName),
            p: common_vendor.t(message.locationAddress),
            q: common_vendor.o(($event) => openLocation(message), index)
          } : message.type === "system" ? {
            s: common_vendor.t(message.content)
          } : {}, {
            f: message.type === "image",
            i: message.type === "voice",
            m: message.type === "location",
            r: message.type === "system",
            t: message.isMine && message.status
          }, message.isMine && message.status ? common_vendor.e({
            v: common_vendor.t(getStatusText(message.status)),
            w: common_vendor.n(message.status),
            x: message.status === "sending"
          }, message.status === "sending" ? {} : message.status === "sent" ? {} : message.status === "read" ? {} : message.status === "failed" ? {} : {}, {
            y: message.status === "sent",
            z: message.status === "read",
            A: message.status === "failed",
            B: common_vendor.n(message.status),
            C: common_vendor.o(($event) => handleStatusClick(message), index)
          }) : {}, {
            D: message.isMine ? 1 : "",
            E: index,
            F: common_vendor.o(($event) => showMessageMenu(message, $event), index)
          });
        }),
        p: scrollTop.value,
        q: common_vendor.o(loadMoreMessages),
        r: isLoadingMore.value,
        s: common_vendor.o(onRefresh),
        t: showMenu.value
      }, showMenu.value ? common_vendor.e({
        v: selectedMessage.value && selectedMessage.value.type === "text"
      }, selectedMessage.value && selectedMessage.value.type === "text" ? {
        w: common_vendor.o(copyMessage)
      } : {}, {
        x: selectedMessage.value && selectedMessage.value.isMine && canRecall(selectedMessage.value)
      }, selectedMessage.value && selectedMessage.value.isMine && canRecall(selectedMessage.value) ? {
        y: common_vendor.o(recallMessage)
      } : {}, {
        z: selectedMessage.value
      }, selectedMessage.value ? {
        A: common_vendor.o(deleteMessage)
      } : {}, {
        B: menuPosition.value.x + "px",
        C: menuPosition.value.y + "px",
        D: common_vendor.o(hideMessageMenu)
      }) : {}, {
        E: showMenu.value
      }, showMenu.value ? {
        F: common_vendor.o(hideMessageMenu)
      } : {}, {
        G: common_vendor.o(toggleSearch),
        H: common_vendor.o(clearChatHistory),
        I: common_vendor.o(exportChatHistory),
        J: showSearchBar.value
      }, showSearchBar.value ? {
        K: common_vendor.o([($event) => searchKeyword.value = $event.detail.value, onSearchInput]),
        L: common_vendor.o(searchMessages),
        M: searchKeyword.value,
        N: common_vendor.o(searchMessages),
        O: common_vendor.o(closeSearch)
      } : {}, {
        P: showQuickReply.value
      }, showQuickReply.value ? {
        Q: common_vendor.o(($event) => showQuickReply.value = false),
        R: common_vendor.o(($event) => sendQuickReply("好的")),
        S: common_vendor.o(($event) => sendQuickReply("收到")),
        T: common_vendor.o(($event) => sendQuickReply("谢谢")),
        U: common_vendor.o(($event) => sendQuickReply("稍等")),
        V: common_vendor.o(($event) => sendQuickReply("没问题")),
        W: common_vendor.o(($event) => sendQuickReply("辛苦了"))
      } : {}, {
        X: showEmojiPanel.value
      }, showEmojiPanel.value ? {
        Y: common_vendor.o(($event) => showEmojiPanel.value = false),
        Z: common_vendor.o(($event) => insertEmoji("😀")),
        aa: common_vendor.o(($event) => insertEmoji("😃")),
        ab: common_vendor.o(($event) => insertEmoji("😄")),
        ac: common_vendor.o(($event) => insertEmoji("😁")),
        ad: common_vendor.o(($event) => insertEmoji("😆")),
        ae: common_vendor.o(($event) => insertEmoji("😅")),
        af: common_vendor.o(($event) => insertEmoji("😂")),
        ag: common_vendor.o(($event) => insertEmoji("🤣")),
        ah: common_vendor.o(($event) => insertEmoji("😊")),
        ai: common_vendor.o(($event) => insertEmoji("😇")),
        aj: common_vendor.o(($event) => insertEmoji("🙂")),
        ak: common_vendor.o(($event) => insertEmoji("🙃")),
        al: common_vendor.o(($event) => insertEmoji("😉")),
        am: common_vendor.o(($event) => insertEmoji("😌")),
        an: common_vendor.o(($event) => insertEmoji("😍")),
        ao: common_vendor.o(($event) => insertEmoji("🥰")),
        ap: common_vendor.o(($event) => insertEmoji("😘")),
        aq: common_vendor.o(($event) => insertEmoji("😗")),
        ar: common_vendor.o(($event) => insertEmoji("😙")),
        as: common_vendor.o(($event) => insertEmoji("😚")),
        at: common_vendor.o(($event) => insertEmoji("😋")),
        av: common_vendor.o(($event) => insertEmoji("😛")),
        aw: common_vendor.o(($event) => insertEmoji("😝")),
        ax: common_vendor.o(($event) => insertEmoji("😜"))
      } : {}, {
        ay: common_vendor.o(($event) => showQuickReply.value = !showQuickReply.value),
        az: common_vendor.o(($event) => showEmojiPanel.value = !showEmojiPanel.value),
        aA: common_assets._imports_2$2,
        aB: common_vendor.o(toggleVoiceInput),
        aC: !isVoiceInput.value
      }, !isVoiceInput.value ? {
        aD: common_vendor.o(sendTextMessage),
        aE: inputFocus.value,
        aF: common_vendor.o([($event) => inputText.value = $event.detail.value, onInputChange]),
        aG: inputText.value
      } : {
        aH: common_vendor.t(isRecording.value ? "松开发送" : "按住说话"),
        aI: common_vendor.o(startRecord),
        aJ: common_vendor.o(stopRecord),
        aK: common_vendor.o(cancelRecord)
      }, {
        aL: common_assets._imports_4$2,
        aM: common_vendor.o(chooseImage),
        aN: common_assets._imports_3$3,
        aO: common_vendor.o(sendLocation),
        aP: inputText.value.trim() && !isVoiceInput.value
      }, inputText.value.trim() && !isVoiceInput.value ? {
        aQ: common_vendor.o(sendTextMessage)
      } : {});
    };
  }
};
wx.createPage(_sfc_main);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/subpkg/chat/chat.js.map
