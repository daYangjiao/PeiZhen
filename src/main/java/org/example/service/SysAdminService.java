package org.example.service;

import org.example.entity.SysAdmin;
import org.example.model.request.AdminCreateSysAdminRequest;
import org.example.model.response.AdminLoginResponse;
import org.example.model.response.PagedResponse;

public interface SysAdminService {

    AdminLoginResponse login(String account, String password);

    AdminLoginResponse loginByWechatIdentity(String platform, String openid, String unionid);

    void bindWechatIdentity(Integer adminId, String platform, String openid, String unionid);

    void bindWechatIdentityToken(Integer adminId, String wechatBindToken);

    PagedResponse<SysAdmin> getAdmins(Integer operatorId, String keyword, Integer status, Integer page, Integer pageSize);

    SysAdmin createAdmin(Integer operatorId, AdminCreateSysAdminRequest request);

    void updateStatus(Integer operatorId, Integer adminId, Integer status);

    void deleteAdmin(Integer operatorId, Integer adminId);

    SysAdmin findById(Integer adminId);
}
