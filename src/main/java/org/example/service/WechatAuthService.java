package org.example.service;

import org.example.model.request.WechatBindPhoneRequest;
import org.example.model.request.WechatLoginRequest;

import java.util.Map;

public interface WechatAuthService {

    Map<String, Object> getConfigStatus(String role);

    String buildUserOAuthUrl(String role, String platform, String redirectUrl);

    String handleUserOAuthCallback(String platform, String code, String state);

    String buildAdminOAuthUrl(String redirectUrl);

    String handleAdminOAuthCallback(String code, String state);

    Map<String, Object> login(WechatLoginRequest request);

    Map<String, Object> bindPhone(WechatBindPhoneRequest request);
}
