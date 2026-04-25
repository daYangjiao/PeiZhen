package org.example.service.impl;

import org.example.dao.AttendantMapper;
import org.example.dao.AttendantQualificationAuditLogMapper;
import org.example.dao.AttendantQualificationMapper;
import org.example.dao.OrderEvaluationMapper;
import org.example.dao.OrderMapper;
import org.example.dao.UserMapper;
import org.example.model.Attendant;
import org.example.model.AttendantQualification;
import org.example.model.User;
import org.example.model.response.AttendantProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendantServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private AttendantMapper attendantMapper;
    @Mock
    private AttendantQualificationMapper attendantQualificationMapper;
    @Mock
    private AttendantQualificationAuditLogMapper auditLogMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderEvaluationMapper orderEvaluationMapper;

    private AttendantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AttendantServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "attendantMapper", attendantMapper);
        ReflectionTestUtils.setField(service, "attendantQualificationMapper", attendantQualificationMapper);
        ReflectionTestUtils.setField(service, "auditLogMapper", auditLogMapper);
        ReflectionTestUtils.setField(service, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "orderEvaluationMapper", orderEvaluationMapper);
    }

    @Test
    void submitQualificationShouldRequireCertificateExpireDates() {
        AttendantQualification qualification = completeQualification();
        qualification.setPracticeCertExpireDate(null);
        when(attendantQualificationMapper.findByUserId(11)).thenReturn(qualification);

        assertThatThrownBy(() -> service.submitQualification(11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请填写执业证书和健康证有效期");
    }

    @Test
    void submitQualificationShouldRejectExpiredCertificate() {
        AttendantQualification qualification = completeQualification();
        qualification.setHealthCertExpireDate(LocalDate.now().minusDays(1).toString());
        when(attendantQualificationMapper.findByUserId(11)).thenReturn(qualification);

        assertThatThrownBy(() -> service.submitQualification(11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("健康证已过期，请重新上传");
    }

    @Test
    void submitQualificationShouldWriteSubmitAuditLog() {
        AttendantQualification qualification = completeQualification();
        Attendant attendant = new Attendant();
        attendant.setUserId(11);
        attendant.setStatus(3);
        when(attendantQualificationMapper.findByUserId(11)).thenReturn(qualification);
        when(attendantMapper.findByUserId(11)).thenReturn(attendant);

        service.submitQualification(11);

        verify(auditLogMapper).insert(org.mockito.ArgumentMatchers.argThat(log ->
                Integer.valueOf(11).equals(log.getUserId())
                        && "ATTENDANT".equals(log.getActorType())
                        && "SUBMIT".equals(log.getAction())
                        && Integer.valueOf(3).equals(log.getFromStatus())
                        && Integer.valueOf(0).equals(log.getToStatus())
        ));
    }

    @Test
    void getProfileShouldNotMarkQualificationUploadedWhenOnlyLegacyFlagExists() {
        User user = new User();
        user.setId(11);
        user.setName("陪诊师");
        user.setStatus(1);
        Attendant attendant = new Attendant();
        attendant.setUserId(11);
        attendant.setStatus(0);
        AttendantQualification qualification = new AttendantQualification();
        qualification.setUserId(11);
        qualification.setIdCardUploaded(1);
        qualification.setPracticeCertUploaded(1);
        qualification.setHealthCertUploaded(1);

        when(userMapper.findById(11)).thenReturn(user);
        when(attendantMapper.findByUserId(11)).thenReturn(attendant);
        when(attendantQualificationMapper.findByUserId(11)).thenReturn(qualification);

        AttendantProfileResponse profile = service.getProfile(11);

        assertThat(profile.getIdCardUploaded()).isFalse();
        assertThat(profile.getPracticeCertUploaded()).isFalse();
        assertThat(profile.getHealthCertUploaded()).isFalse();
    }

    @Test
    void getProfileShouldUseScanQualificationImagesAsDisplayFallback() {
        User user = new User();
        user.setId(11);
        user.setName("陪诊师");
        user.setStatus(1);
        Attendant attendant = new Attendant();
        attendant.setUserId(11);
        attendant.setStatus(0);
        AttendantQualification qualification = new AttendantQualification();
        qualification.setUserId(11);
        qualification.setIdCardFrontScanFileUrl("front-scan.png");
        qualification.setIdCardBackScanFileUrl("back-scan.png");
        qualification.setPracticeCertScanFileUrl("practice-scan.png");
        qualification.setHealthCertScanFileUrl("health-scan.png");

        when(userMapper.findById(11)).thenReturn(user);
        when(attendantMapper.findByUserId(11)).thenReturn(attendant);
        when(attendantQualificationMapper.findByUserId(11)).thenReturn(qualification);

        AttendantProfileResponse profile = service.getProfile(11);

        assertThat(profile.getIdCardUploaded()).isTrue();
        assertThat(profile.getPracticeCertUploaded()).isTrue();
        assertThat(profile.getHealthCertUploaded()).isTrue();
        assertThat(profile.getIdCardFrontFileUrl()).isEqualTo("front-scan.png");
        assertThat(profile.getIdCardBackFileUrl()).isEqualTo("back-scan.png");
        assertThat(profile.getPracticeCertFileUrl()).isEqualTo("practice-scan.png");
        assertThat(profile.getHealthCertFileUrl()).isEqualTo("health-scan.png");
    }

    private AttendantQualification completeQualification() {
        AttendantQualification qualification = new AttendantQualification();
        qualification.setUserId(11);
        qualification.setIdCardFrontFileUrl("front.png");
        qualification.setIdCardBackFileUrl("back.png");
        qualification.setPracticeCertFileUrl("practice.png");
        qualification.setHealthCertFileUrl("health.png");
        qualification.setPracticeCertExpireDate(LocalDate.now().plusYears(1).toString());
        qualification.setHealthCertExpireDate(LocalDate.now().plusYears(1).toString());
        return qualification;
    }
}
