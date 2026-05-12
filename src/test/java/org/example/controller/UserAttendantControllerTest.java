package org.example.controller;

import org.example.common.ResponseResult;
import org.example.model.response.AttendantProfileResponse;
import org.example.service.AttendantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAttendantControllerTest {

    @Mock
    private AttendantService attendantService;

    @Test
    void getAttendantDetailShouldReturnAggregatedPraiseRateAndEvaluationCount() {
        AttendantProfileResponse profile = new AttendantProfileResponse();
        profile.setId(21);
        profile.setName("李陪诊");
        profile.setPhone("13800000000");
        profile.setScore(4.0);
        profile.setEvaluationCount(3);
        profile.setPraiseRate(67);

        when(attendantService.getProfile(21)).thenReturn(profile);

        UserAttendantController controller = new UserAttendantController(attendantService);
        ResponseResult<Map<String, Object>> response = controller.getAttendantDetail(21);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).containsEntry("score", 4.0D);
        assertThat(response.getData()).containsEntry("evaluationCount", 3);
        assertThat(response.getData()).containsEntry("praiseRate", 67);
    }

    @Test
    void getAttendantDetailShouldReturnQualificationPreviewUrls() {
        AttendantProfileResponse profile = new AttendantProfileResponse();
        profile.setId(21);
        profile.setName("李陪诊");
        profile.setQualificationStatusCode(1);
        profile.setQualificationStatusText("已通过");
        profile.setIdCardUploaded(true);
        profile.setPracticeCertUploaded(true);
        profile.setHealthCertUploaded(true);
        profile.setIdCardFrontFileUrl("/uploads/front.jpg");
        profile.setIdCardBackFileUrl("/uploads/back.jpg");
        profile.setPracticeCertFileUrl("/uploads/practice.jpg");
        profile.setHealthCertFileUrl("/uploads/health.jpg");

        when(attendantService.getProfile(21)).thenReturn(profile);

        UserAttendantController controller = new UserAttendantController(attendantService);
        ResponseResult<Map<String, Object>> response = controller.getAttendantDetail(21);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData())
                .containsEntry("qualificationStatusCode", 1)
                .containsEntry("qualificationStatusText", "已通过")
                .containsEntry("idCardUploaded", true)
                .containsEntry("practiceCertUploaded", true)
                .containsEntry("healthCertUploaded", true)
                .containsEntry("idCardFrontFileUrl", "/uploads/front.jpg")
                .containsEntry("idCardBackFileUrl", "/uploads/back.jpg")
                .containsEntry("practiceCertFileUrl", "/uploads/practice.jpg")
                .containsEntry("healthCertFileUrl", "/uploads/health.jpg");
    }
}
