package org.example.util;

import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.User;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class AttendantQualificationPolicy {

    private AttendantQualificationPolicy() {}

    public static int completeness(AttendantQualification qualification) {
        if (qualification == null) {
            return 0;
        }
        int completed = 0;
        if (hasText(firstNonBlank(qualification.getIdCardFrontFileUrl(), qualification.getIdCardFileUrl()))) completed++;
        if (hasText(qualification.getIdCardBackFileUrl())) completed++;
        if (hasText(qualification.getPracticeCertFileUrl())) completed++;
        if (hasText(qualification.getHealthCertFileUrl())) completed++;
        if (hasText(qualification.getPracticeCertExpireDate())) completed++;
        if (hasText(qualification.getHealthCertExpireDate())) completed++;
        return completed * 100 / 6;
    }

    public static boolean isComplete(AttendantQualification qualification) {
        return qualification != null
                && hasText(firstNonBlank(qualification.getIdCardFrontFileUrl(), qualification.getIdCardFileUrl()))
                && hasText(qualification.getIdCardBackFileUrl())
                && hasText(qualification.getPracticeCertFileUrl())
                && hasText(qualification.getHealthCertFileUrl());
    }

    public static void requireSubmittable(AttendantQualification qualification) {
        if (!isComplete(qualification)) {
            throw new IllegalArgumentException("请先上传身份证、执业证书和健康证");
        }
        if (!hasText(qualification.getPracticeCertExpireDate()) || !hasText(qualification.getHealthCertExpireDate())) {
            throw new IllegalArgumentException("请填写执业证书和健康证有效期");
        }
        if (isExpired(qualification.getPracticeCertExpireDate())) {
            throw new IllegalArgumentException("执业证书已过期，请重新上传");
        }
        if (isExpired(qualification.getHealthCertExpireDate())) {
            throw new IllegalArgumentException("健康证已过期，请重新上传");
        }
    }

    public static String acceptBlockReason(User user, Attendant attendant, AttendantQualification qualification) {
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            return "账号已被禁用，请联系平台客服处理";
        }
        if (attendant == null) {
            return "陪诊师资料不存在";
        }
        Integer status = attendant.getStatus() == null ? 0 : attendant.getStatus();
        if (status == 0) {
            return "资质正在审核中，请等待平台审核通过";
        }
        if (status == 2) {
            return hasText(attendant.getQualificationFailReason())
                    ? attendant.getQualificationFailReason()
                    : "账号已封禁，请联系平台客服处理";
        }
        if (status == 3) {
            return hasText(attendant.getQualificationFailReason())
                    ? attendant.getQualificationFailReason()
                    : "资质审核未通过，请修改后重新提交";
        }
        try {
            requireSubmittable(qualification);
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
        return "";
    }

    public static boolean canAcceptOrders(User user, Attendant attendant, AttendantQualification qualification) {
        return attendant != null
                && Integer.valueOf(1).equals(attendant.getStatus())
                && acceptBlockReason(user, attendant, qualification).isEmpty();
    }

    public static boolean isExpired(String dateValue) {
        if (!hasText(dateValue)) {
            return false;
        }
        try {
            return LocalDate.parse(dateValue.trim()).isBefore(LocalDate.now());
        } catch (DateTimeParseException ex) {
            return true;
        }
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static String firstNonBlank(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }
}
