package org.example.service;

import org.example.model.request.WechatBindPhoneRequest;
import org.example.model.request.WechatLoginRequest;

import java.util.Map;

public interface WechatAuthService {

    Map<String, Object> getConfigStatus(String role);

    Map<String, Object> login(WechatLoginRequest request);

    Map<String, Object> bindPhone(WechatBindPhoneRequest request);
}
