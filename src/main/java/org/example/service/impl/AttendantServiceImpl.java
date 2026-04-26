package org.example.service.impl;

import org.example.dao.AttendantMapper;
import org.example.dao.AttendantQualificationAuditLogMapper;
import org.example.dao.AttendantQualificationMapper;
import org.example.dao.OrderEvaluationMapper;
import org.example.dao.OrderMapper;
import org.example.dao.UserMapper;
import org.example.model.Attendant;
import org.example.model.AttendantQualificationAuditLog;
import org.example.model.AttendantQualification;
import org.example.model.User;
import org.example.model.response.AttendantProfileResponse;
import org.example.model.response.AttendantQualificationLogResponse;
import org.example.model.response.AttendantRatingSummary;
import org.example.service.AttendantService;
import org.example.util.AttendantQualificationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

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
    private AttendantQualificationAuditLogMapper auditLogMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderEvaluationMapper orderEvaluationMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.save(user);
        Integer userId = user.getId();
        logger.info("创建陪诊师基础账号成功, User ID: {}", userId);

        // 3. 创建 Attendant 扩展信息
        attendant.setUserId(userId);
        if (attendant.getStatus() == null) {
            attendant.setStatus(1);
        }
        if (attendant.getQualificationStatus() == null) {
            attendant.setQualificationStatus(0);
        }
        attendantMapper.insert(attendant);
        logger.info("创建陪诊师扩展信息成功, User ID: {}", userId);

        // 4. 初始化陪诊师资质信息，后续资质页直接进入编辑而不是空状态
        attendantQualificationMapper.insert(createDefaultQualification(userId));
        logger.info("初始化陪诊师资质信息成功, User ID: {}", userId);

        return userId;
    }

    @Override
    public Attendant findByUserId(Integer userId) {
        return applyActualRating(attendantMapper.findByUserId(userId));
    }

    @Override
    public int update(Attendant attendant) {
        return attendantMapper.update(attendant);
    }
    
    @Override
    public List<Attendant> findRecommended() {
        // 简单实现：查找所有状态正常的陪诊师
        // 实际业务中可以根据评分、接单量等更复杂的逻辑来推荐
        return applyActualRating(attendantMapper.findRecommended());
    }

    @Override
    public List<Attendant> findAiCandidates(int limit) {
        int safeLimit = Math.max(3, Math.min(limit, 30));
        return applyActualRating(attendantMapper.findAiCandidates(safeLimit));
    }

    @Override
    public AttendantRatingSummary getRatingSummary(Integer userId) {
        if (userId == null) {
            return new AttendantRatingSummary(null, 0, 0);
        }
        Integer totalEvalCount = orderEvaluationMapper.countByAttendantId(userId);
        int evaluationCount = totalEvalCount == null ? 0 : totalEvalCount;
        if (evaluationCount <= 0) {
            return new AttendantRatingSummary(null, 0, 0);
        }
        Integer goodEvalCount = orderEvaluationMapper.countGoodByAttendantId(userId, 4);
        BigDecimal averageRating = orderEvaluationMapper.averageRatingByAttendantId(userId);
        int praiseRate = (int) Math.round((goodEvalCount == null ? 0 : goodEvalCount) * 100.0 / evaluationCount);
        return new AttendantRatingSummary(normalizeRatingScore(averageRating), evaluationCount, praiseRate);
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

        AttendantRatingSummary ratingSummary = getRatingSummary(userId);
        Attendant attendant = applyActualRating(attendantMapper.findByUserId(userId), ratingSummary);
        if (attendant != null) {
            response.setCertificate(attendant.getCertificate());
            response.setScore(attendant.getScore());
            response.setIntroduction(attendant.getIntroduction());
            response.setProfessionalField(attendant.getProfessionalField());
            response.setExperienceYears(attendant.getExperienceYears());
            response.setHospitalName(attendant.getHospitalName());
            Integer qualificationStatus = qualificationStatus(attendant);
            response.setQualificationStatusCode(qualificationStatus);
            response.setQualificationStatusText(mapQualificationStatusText(qualificationStatus));
            if (qualificationStatus != null && qualificationStatus == 2) {
                String failReason = attendant.getQualificationFailReason();
                response.setQualificationFailReason(
                        failReason == null || failReason.trim().isEmpty()
                                ? "资质审核未通过，请联系客服处理"
                                : failReason
                );
            }
        } else {
            response.setScore(null);
            response.setQualificationStatusCode(0);
            response.setQualificationStatusText(mapQualificationStatusText(0));
        }

        AttendantQualification qualification = attendantQualificationMapper.findByUserId(userId);
        if (qualification != null) {
            String idCardFrontFileUrl = firstNonBlank(
                    firstNonBlank(qualification.getIdCardFrontFileUrl(), qualification.getIdCardFileUrl()),
                    qualification.getIdCardFrontScanFileUrl()
            );
            String idCardBackFileUrl = firstNonBlank(qualification.getIdCardBackFileUrl(), qualification.getIdCardBackScanFileUrl());
            String practiceCertFileUrl = firstNonBlank(qualification.getPracticeCertFileUrl(), qualification.getPracticeCertScanFileUrl());
            String healthCertFileUrl = firstNonBlank(qualification.getHealthCertFileUrl(), qualification.getHealthCertScanFileUrl());
            boolean idCardUploaded = hasText(idCardFrontFileUrl) && hasText(idCardBackFileUrl);
            boolean practiceCertUploaded = hasText(practiceCertFileUrl);
            boolean healthCertUploaded = hasText(healthCertFileUrl);

            response.setIdCardUploaded(idCardUploaded);
            response.setPracticeCertUploaded(practiceCertUploaded);
            response.setHealthCertUploaded(healthCertUploaded);
            response.setIdCardFileUrl(idCardFrontFileUrl);
            response.setIdCardFrontFileUrl(idCardFrontFileUrl);
            response.setIdCardFrontScanFileUrl(firstNonBlank(qualification.getIdCardFrontScanFileUrl(), idCardFrontFileUrl));
            response.setIdCardBackFileUrl(idCardBackFileUrl);
            response.setIdCardBackScanFileUrl(firstNonBlank(qualification.getIdCardBackScanFileUrl(), idCardBackFileUrl));
            response.setPracticeCertFileUrl(practiceCertFileUrl);
            response.setPracticeCertScanFileUrl(firstNonBlank(qualification.getPracticeCertScanFileUrl(), practiceCertFileUrl));
            response.setHealthCertFileUrl(healthCertFileUrl);
            response.setHealthCertScanFileUrl(firstNonBlank(qualification.getHealthCertScanFileUrl(), healthCertFileUrl));
            response.setPracticeCertExpireDate(qualification.getPracticeCertExpireDate());
            response.setHealthCertExpireDate(qualification.getHealthCertExpireDate());
            response.setPracticeCertExpired(AttendantQualificationPolicy.isExpired(qualification.getPracticeCertExpireDate()));
            response.setHealthCertExpired(AttendantQualificationPolicy.isExpired(qualification.getHealthCertExpireDate()));
            response.setQualificationCompleteness(AttendantQualificationPolicy.completeness(qualification));
        } else {
            response.setIdCardUploaded(false);
            response.setPracticeCertUploaded(false);
            response.setHealthCertUploaded(false);
            response.setIdCardFileUrl("");
            response.setIdCardFrontFileUrl("");
            response.setIdCardFrontScanFileUrl("");
            response.setIdCardBackFileUrl("");
            response.setIdCardBackScanFileUrl("");
            response.setPracticeCertFileUrl("");
            response.setPracticeCertScanFileUrl("");
            response.setHealthCertFileUrl("");
            response.setHealthCertScanFileUrl("");
            response.setPracticeCertExpireDate("");
            response.setHealthCertExpireDate("");
            response.setPracticeCertExpired(false);
            response.setHealthCertExpired(false);
            response.setQualificationCompleteness(0);
        }

        String blockReason = AttendantQualificationPolicy.acceptBlockReason(user, attendant, qualification);
        response.setCanAcceptOrders(blockReason.isEmpty());
        response.setQualificationBlockReason(blockReason);
        Integer qualificationStatus = qualificationStatus(attendant);
        response.setQualificationPopupRequired(attendant != null
                && (Integer.valueOf(0).equals(user.getStatus()) || Integer.valueOf(2).equals(qualificationStatus)));
        response.setRecentQualificationLogs(toAttendantLogs(auditLogMapper.findLatestByUserId(userId, 5)));

        Integer todayService = orderMapper.countTodayCompletedService(userId);
        Integer monthService = orderMapper.countMonthCompletedService(userId);
        BigDecimal totalIncome = orderMapper.sumCompletedIncome(userId);
        Integer totalOrders = orderMapper.countTotalOrdersByAttendant(userId);
        Integer completedOrders = orderMapper.countCompletedOrdersByAttendant(userId);

        BigDecimal normalizedIncome = totalIncome == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalIncome.setScale(2, RoundingMode.HALF_UP);

        response.setTodayService(todayService == null ? 0 : todayService);
        response.setMonthService(monthService == null ? 0 : monthService);
        response.setTotalIncome(normalizedIncome);
        response.setPraiseRate(ratingSummary.getPraiseRate());
        response.setEvaluationCount(ratingSummary.getEvaluationCount());

        // 当前无提现流水表，余额按“已完成订单最终金额扣除平台服务费后的收入”口径返回。
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

        Attendant attendant = attendantMapper.findByUserId(userId);
        if (existing == null) {
            int rows = attendantQualificationMapper.insert(target);
            writeAttendantLog(userId, "UPLOAD", qualificationStatus(attendant), qualificationStatus(attendant), "上传资质材料", target);
            return rows;
        }
        int rows = attendantQualificationMapper.updateByUserId(target);
        writeAttendantLog(userId, "UPLOAD", qualificationStatus(attendant), qualificationStatus(attendant), "上传资质材料", target);
        return rows;
    }

    @Override
    @Transactional
    public String submitQualification(Integer userId) {
        AttendantQualification qualification = attendantQualificationMapper.findByUserId(userId);
        AttendantQualificationPolicy.requireSubmittable(qualification);

        Attendant attendant = attendantMapper.findByUserId(userId);
        if (attendant == null) {
            throw new IllegalArgumentException("陪诊师信息不存在");
        }

        Attendant update = new Attendant();
        update.setUserId(userId);
        update.setQualificationStatus(0);
        update.setQualificationFailReason("");
        attendantMapper.update(update);
        writeAttendantLog(userId, "SUBMIT", qualificationStatus(attendant), 0, "提交资质审核", qualification);
        return "提交审核成功";
    }

    @Override
    public List<Attendant> getAdminList(String keyword, Integer auditStatus) {
        return attendantMapper.findAdminAttendants(keyword, auditStatus, 0, 200);
    }

    private String mapQualificationStatusText(Integer status) {
        if (status == null) {
            return "待审核";
        }
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "未通过";
            default -> "待审核";
        };
    }

    private Integer qualificationStatus(Attendant attendant) {
        if (attendant == null) {
            return 0;
        }
        if (attendant.getQualificationStatus() != null) {
            return attendant.getQualificationStatus();
        }
        Integer legacyStatus = attendant.getStatus();
        if (legacyStatus == null) {
            return 0;
        }
        if (legacyStatus == 1) {
            return 1;
        }
        if (legacyStatus == 2 || legacyStatus == 3) {
            return 2;
        }
        return 0;
    }

    private List<Attendant> applyActualRating(List<Attendant> attendants) {
        if (attendants == null || attendants.isEmpty()) {
            return attendants;
        }
        attendants.forEach(this::applyActualRating);
        return attendants;
    }

    private Attendant applyActualRating(Attendant attendant) {
        return applyActualRating(attendant, attendant == null ? null : getRatingSummary(attendant.getUserId()));
    }

    private Attendant applyActualRating(Attendant attendant, AttendantRatingSummary summary) {
        if (attendant == null || attendant.getUserId() == null) {
            return attendant;
        }
        AttendantRatingSummary ratingSummary = summary == null ? getRatingSummary(attendant.getUserId()) : summary;
        attendant.setScore(ratingSummary.getScore());
        attendant.setEvaluationCount(ratingSummary.getEvaluationCount());
        attendant.setPraiseRate(ratingSummary.getPraiseRate());
        return attendant;
    }

    private BigDecimal normalizeRatingScore(BigDecimal score) {
        return score == null ? null : score.setScale(1, RoundingMode.HALF_UP);
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
        if (incoming.getIdCardFrontScanFileUrl() != null) {
            target.setIdCardFrontScanFileUrl(incoming.getIdCardFrontScanFileUrl());
        }
        if (incoming.getIdCardBackFileUrl() != null) {
            target.setIdCardBackFileUrl(incoming.getIdCardBackFileUrl());
        }
        if (incoming.getIdCardBackScanFileUrl() != null) {
            target.setIdCardBackScanFileUrl(incoming.getIdCardBackScanFileUrl());
        }
        if (incoming.getPracticeCertFileUrl() != null) {
            target.setPracticeCertFileUrl(incoming.getPracticeCertFileUrl());
        }
        if (incoming.getPracticeCertScanFileUrl() != null) {
            target.setPracticeCertScanFileUrl(incoming.getPracticeCertScanFileUrl());
        }
        if (incoming.getHealthCertFileUrl() != null) {
            target.setHealthCertFileUrl(incoming.getHealthCertFileUrl());
        }
        if (incoming.getHealthCertScanFileUrl() != null) {
            target.setHealthCertScanFileUrl(incoming.getHealthCertScanFileUrl());
        }
        if (incoming.getPracticeCertExpireDate() != null) {
            target.setPracticeCertExpireDate(incoming.getPracticeCertExpireDate());
        }
        if (incoming.getHealthCertExpireDate() != null) {
            target.setHealthCertExpireDate(incoming.getHealthCertExpireDate());
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

    private void writeAttendantLog(Integer userId, String action, Integer fromStatus, Integer toStatus, String reason, AttendantQualification qualification) {
        AttendantQualificationAuditLog log = new AttendantQualificationAuditLog();
        log.setUserId(userId);
        log.setActorType("ATTENDANT");
        log.setActorId(userId);
        User user = userMapper.findById(userId);
        if (user != null) {
            log.setActorName(user.getName());
            log.setActorPhone(user.getPhone());
        }
        log.setActorRole("ATTENDANT");
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setReason(reason);
        log.setSnapshotJson(buildQualificationSnapshot(qualification));
        auditLogMapper.insert(log);
    }

    private String buildQualificationSnapshot(AttendantQualification qualification) {
        if (qualification == null) {
            return "{}";
        }
        return "{"
                + "\"idCardFrontFileUrl\":\"" + escapeJson(qualification.getIdCardFrontFileUrl()) + "\","
                + "\"idCardFrontScanFileUrl\":\"" + escapeJson(qualification.getIdCardFrontScanFileUrl()) + "\","
                + "\"idCardBackFileUrl\":\"" + escapeJson(qualification.getIdCardBackFileUrl()) + "\","
                + "\"idCardBackScanFileUrl\":\"" + escapeJson(qualification.getIdCardBackScanFileUrl()) + "\","
                + "\"practiceCertFileUrl\":\"" + escapeJson(qualification.getPracticeCertFileUrl()) + "\","
                + "\"practiceCertScanFileUrl\":\"" + escapeJson(qualification.getPracticeCertScanFileUrl()) + "\","
                + "\"healthCertFileUrl\":\"" + escapeJson(qualification.getHealthCertFileUrl()) + "\","
                + "\"healthCertScanFileUrl\":\"" + escapeJson(qualification.getHealthCertScanFileUrl()) + "\","
                + "\"practiceCertExpireDate\":\"" + escapeJson(qualification.getPracticeCertExpireDate()) + "\","
                + "\"healthCertExpireDate\":\"" + escapeJson(qualification.getHealthCertExpireDate()) + "\""
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private List<AttendantQualificationLogResponse> toAttendantLogs(List<AttendantQualificationAuditLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        return logs.stream().map(log -> {
            AttendantQualificationLogResponse response = new AttendantQualificationLogResponse();
            response.setAction(log.getAction());
            response.setFromStatus(log.getFromStatus());
            response.setToStatus(log.getToStatus());
            response.setReason(log.getReason());
            response.setCreateTime(log.getCreateTime());
            return response;
        }).collect(Collectors.toList());
    }
}
