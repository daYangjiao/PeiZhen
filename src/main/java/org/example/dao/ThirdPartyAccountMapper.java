package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.ThirdPartyAccount;

@Mapper
public interface ThirdPartyAccountMapper {

    ThirdPartyAccount findUserAccountByWechat(@Param("platform") String platform,
                                              @Param("openid") String openid,
                                              @Param("unionid") String unionid);

    ThirdPartyAccount findAdminAccountByWechat(@Param("platform") String platform,
                                               @Param("openid") String openid,
                                               @Param("unionid") String unionid);

    int upsert(ThirdPartyAccount account);
}
