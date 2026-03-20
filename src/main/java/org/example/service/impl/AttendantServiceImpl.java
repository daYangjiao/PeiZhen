package org.example.service.impl;

import org.example.dao.AttendantMapper;
import org.example.dao.AttendantQualificationMapper;
import org.example.dao.OrderEvaluationMapper;
import org.example.dao.OrderMapper;
import org.example.dao.UserMapper;
import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.User;
import org.example.model.response.AttendantProfileResponse;
import org.example.service.AttendantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AttendantServiceImpl implements AttendantService {
    private static final Logger logger = LoggerFactory.getLogger(AttendantServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AttendantMapper attendantMapper;

    @Autowired
    private AttendantQualificationMapper attendantQualificationMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderEvaluationMapper orderEvaluationMapper;

    @Override
    @Transactional
    public int registerAttendant(User user, Attendant attendant) {
        // 1. 校验手机号是否已存在
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (userMapper.findByPhone(user.getPhone()) != null) {
            throw new IllegalArgumentException("手机号已存在");
        }
        
        // 2. 创建 User 账号，并设置为陪诊师角色
        user.setUserType(1); 
        userMapper.save(user);
        Integer userId = user.getId();
        logger.info("创建陪诊师基础账号成功, User ID: {}", userId);

        // 3. 创建 Attendant 扩展信息
        attendant.setUserId(userId);
        attendantMapper.insert(attendant);
        logger.info("创建陪诊师扩展信息成功, User ID: {}", userId);

        return userId;
    }

    @Override
    public Attendant findByUserId(Integer userId) {
        return attendantMapper.findByUserId(userId);
    }

    @Override
    public int update(Attendant attendant) {
        return attendantMapper.update(attendant);
    }
    
    @Override
    public List<Attendant> findRecommended() {
        // 简单实现：查找所有状态正常的陪诊师
        // 实际业务中可以根据评分、接单量等更复杂的逻辑来推荐
        return attendantMapper.findRecommended();
    }

    @Override
    public AttendantProfileResponse getProfile(Integer userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return null;
        }

        AttendantProfileResponse response = new AttendantProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName() != null ? user.getName() : user.getPhone());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatar());

        Attendant attendant = attendantMapper.findByUserId(userId);
        if (attendant != null) {
            response.setCertificate(attendant.getCertificate());
            response.setScore(attendant.getScore());
            response.setIntroduction(attendant.getIntroduction());
            response.setProfessionalField(attendant.getProfessionalField());
            response.setExperienceYears(attendant.getExperienceYears());
            response.setHospitalName(attendant.getHospitalName());
            response.setQualificationStatusCode(attendant.getStatus() == null ? 0 : attendant.getStatus());
            response.setQualificationStatusText(mapQualificationStatusText(attendant.getStatus()));
            if (attendant.getStatus() != null && attendant.getStatus() == 3) {
                String failReason = attendant.getQualificationFailReason();
                response.setQualificationFailReason(
                        failReason == null || failReason.trim().isEmpty()
                                ? "资质审核未通过，请联系客服处理"
                                : failReason
                );
            }
        } else {
            response.setQualificationStatusCode(0);
            response.setQualificationStatusText(mapQualificationStatusText(0));
        }

        AttendantQualification qualification = attendantQualificationMapper.findByUserId(userId);
        if (qualification != null) {
            String idCardFrontFileUrl = firstNonBlank(qualification.getIdCardFrontFileUrl(), qualification.getIdCardFileUrl());
            String idCardBackFileUrl = qualification.getIdCardBackFileUrl();
            boolean idCardUploaded = hasText(idCardFrontFileUrl) && hasText(idCardBackFileUrl);
            boolean practiceCertUploaded = toBoolean(qualification.getPracticeCertUploaded()) || hasText(qualification.getPracticeCertFileUrl());
            boolean healthCertUploaded = toBoolean(qualification.getHealthCertUploaded()) || hasText(qualification.getHealthCertFileUrl());

            response.setIdCardUploaded(idCardUploaded);
            response.setPracticeCertUploaded(practiceCertUploaded);
            response.setHealthCertUploaded(healthCertUploaded);
            response.setIdCardFileUrl(idCardFrontFileUrl);
            response.setIdCardFrontFileUrl(idCardFrontFileUrl);
            response.setIdCardBackFileUrl(idCardBackFileUrl);
            response.setPracticeCertFileUrl(qualification.getPracticeCertFileUrl());
            response.setHealthCertFileUrl(qualification.getHealthCertFileUrl());
        } else {
            response.setIdCardUploaded(false);
            response.setPracticeCertUploaded(false);
            response.setHealthCertUploaded(false);
            response.setIdCardFileUrl("");
            response.setIdCardFrontFileUrl("");
            response.setIdCardBackFileUrl("");
            response.setPracticeCertFileUrl("");
            response.setHealthCertFileUrl("");
        }

        Integer todayService = orderMapper.countTodayCompletedService(userId);
        Integer monthService = orderMapper.countMonthCompletedService(userId);
        BigDecimal totalIncome = orderMapper.sumCompletedIncome(userId);
        Integer totalOrders = orderMapper.countTotalOrdersByAttendant(userId);
        Integer completedOrders = orderMapper.countCompletedOrdersByAttendant(userId);

        Integer totalEvalCount = orderEvaluationMapper.countByAttendantId(userId);
        Integer goodEvalCount = orderEvaluationMapper.countGoodByAttendantId(userId, 4);
        int praiseRate = 0;
        if (totalEvalCount != null && totalEvalCount > 0) {
            praiseRate = (int) Math.round((goodEvalCount == null ? 0 : goodEvalCount) * 100.0 / totalEvalCount);
        }

        BigDecimal normalizedIncome = totalIncome == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalIncome.setScale(2, RoundingMode.HALF_UP);

        response.setTodayService(todayService == null ? 0 : todayService);
        response.setMonthService(monthService == null ? 0 : monthService);
        response.setTotalIncome(normalizedIncome);
        response.setPraiseRate(praiseRate);

        // 当前无提现流水表，余额按“已完成订单收入（含差价结算）”口径返回。
        response.setBalance(normalizedIncome);

        // 兼容旧版字段，避免前端灰度期间出现空值
        response.setTotalOrders(totalOrders == null ? 0 : totalOrders);
        response.setCompletedOrders(completedOrders == null ? 0 : completedOrders);
        response.setTotalEarnings(normalizedIncome);
        return response;
    }

    @Override
    @Transactional
    public int updateQualification(Integer userId, AttendantQualification qualification) {
        AttendantQualification existing = attendantQualificationMapper.findByUserId(userId);
        AttendantQualification target = existing == null ? createDefaultQualification(userId) : existing;

        mergeQualification(target, qualification);

        if (existing == null) {
            return attendantQualificationMapper.insert(target);
        }
        return attendantQualificationMapper.updateByUserId(target);
    }

    @Override
    @Transactional
    public String submitQualification(Integer userId) {
        AttendantQualification qualification = attendantQualificationMapper.findByUserId(userId);
        if (qualification == null
                || !isIdCardCompleted(qualification)
                || !hasText(qualification.getPracticeCertFileUrl())
                || !hasText(qualification.getHealthCertFileUrl())) {
            throw new IllegalArgumentException("请先上传身份证、执业证书和健康证");
        }

        Attendant attendant = attendantMapper.findByUserId(userId);
        if (attendant == null) {
            throw new IllegalArgumentException("陪诊师信息不存在");
        }

        Attendant update = new Attendant();
        update.setUserId(userId);
        update.setStatus(0);
        update.setQualificationFailReason("");
        attendantMapper.update(update);
        return "提交审核成功";
    }

    private String mapQualificationStatusText(Integer status) {
        if (status == null) {
            return "待审核";
        }
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已审核";
            case 2 -> "封禁";
            case 3 -> "审核失败";
            default -> "待审核";
        };
    }

    private boolean toBoolean(Integer flag) {
        return flag != null && flag == 1;
    }

    private AttendantQualification createDefaultQualification(Integer userId) {
        AttendantQualification qualification = new AttendantQualification();
        qualification.setUserId(userId);
        qualification.setIdCardUploaded(0);
        qualification.setPracticeCertUploaded(0);
        qualification.setHealthCertUploaded(0);
        return qualification;
    }

    private void mergeQualification(AttendantQualification target, AttendantQualification incoming) {
        if (incoming == null) {
            return;
        }
        if (incoming.getPracticeCertUploaded() != null) {
            target.setPracticeCertUploaded(incoming.getPracticeCertUploaded());
        }
        if (incoming.getHealthCertUploaded() != null) {
            target.setHealthCertUploaded(incoming.getHealthCertUploaded());
        }
        if (incoming.getIdCardFileUrl() != null) {
            target.setIdCardFileUrl(incoming.getIdCardFileUrl());
            if (!hasText(target.getIdCardFrontFileUrl()) && hasText(incoming.getIdCardFileUrl())) {
                target.setIdCardFrontFileUrl(incoming.getIdCardFileUrl());
            }
        }
        if (incoming.getIdCardFrontFileUrl() != null) {
            target.setIdCardFrontFileUrl(incoming.getIdCardFrontFileUrl());
            if (hasText(incoming.getIdCardFrontFileUrl())) {
                target.setIdCardFileUrl(incoming.getIdCardFrontFileUrl());
            }
        }
        if (incoming.getIdCardBackFileUrl() != null) {
            target.setIdCardBackFileUrl(incoming.getIdCardBackFileUrl());
        }
        if (incoming.getPracticeCertFileUrl() != null) {
            target.setPracticeCertFileUrl(incoming.getPracticeCertFileUrl());
        }
        if (incoming.getHealthCertFileUrl() != null) {
            target.setHealthCertFileUrl(incoming.getHealthCertFileUrl());
        }

        target.setIdCardUploaded(isIdCardCompleted(target) ? 1 : 0);
        target.setPracticeCertUploaded((toBoolean(target.getPracticeCertUploaded()) || hasText(target.getPracticeCertFileUrl())) ? 1 : 0);
        target.setHealthCertUploaded((toBoolean(target.getHealthCertUploaded()) || hasText(target.getHealthCertFileUrl())) ? 1 : 0);
    }

    private boolean isIdCardCompleted(AttendantQualification qualification) {
        if (qualification == null) {
            return false;
        }
        String front = firstNonBlank(qualification.getIdCardFrontFileUrl(), qualification.getIdCardFileUrl());
        return hasText(front) && hasText(qualification.getIdCardBackFileUrl());
    }

    private String firstNonBlank(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
