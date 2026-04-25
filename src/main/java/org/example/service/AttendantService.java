package org.example.service;

import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.response.AttendantProfileResponse;
import org.example.model.response.AttendantRatingSummary;
import org.example.model.User;
import java.util.List;

public interface AttendantService {

    /**
     * 注册成为陪诊师 (包含创建User账号和Attendant扩展信息)
     * @param user 包含用户名、密码等基本信息
     * @param attendant 包含专业领域、简介等扩展信息
     * @return 创建的用户ID
     */
    int registerAttendant(User user, Attendant attendant);

    /**
     * 根据用户ID获取陪诊师扩展信息
     * @param userId 用户ID
     * @return 陪诊师扩展信息
     */
    Attendant findByUserId(Integer userId);

    /**
     * 更新陪诊师扩展信息
     * @param attendant 待更新的扩展信息
     * @return 更新行数
     */
    int update(Attendant attendant);

    /**
     * 查找推荐的陪诊师列表 (例如，用于首页展示)
     * @return 推荐的陪诊师列表
     */
    List<Attendant> findRecommended();

    /**
     * AI 匹配候选陪诊师
     */
    List<Attendant> findAiCandidates(int limit);

    AttendantRatingSummary getRatingSummary(Integer userId);

    /**
     * 获取陪诊师个人中心资料
     * @param userId 陪诊师用户ID
     * @return 个人中心资料
     */
    AttendantProfileResponse getProfile(Integer userId);

    /**
     * 更新陪诊师三证资质信息
     * @param userId 用户ID
     * @param qualification 三证信息
     * @return 更新行数
     */
    int updateQualification(Integer userId, AttendantQualification qualification);

    /**
     * 提交资质审核
     * @param userId 用户ID
     * @return 提交结果
     */
    String submitQualification(Integer userId);

    List<Attendant> getAdminList(String keyword, Integer auditStatus);
}
