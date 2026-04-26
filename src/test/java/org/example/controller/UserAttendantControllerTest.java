package org.example.controller;

import org.example.common.ResponseResult;
import org.example.model.Attendant;
import org.example.model.User;
import org.example.service.AttendantService;
import org.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAttendantControllerTest {

    @Mock
    private AttendantService attendantService;
    @Mock
    private UserService userService;

    @Test
    void getAttendantDetailShouldReturnAggregatedPraiseRateAndEvaluationCount() {
        User user = new User();
        user.setId(21);
        user.setName("李陪诊");
        user.setPhone("13800000000");

        Attendant attendant = new Attendant();
        attendant.setUserId(21);
        attendant.setScore(new BigDecimal("4.0"));
        attendant.setEvaluationCount(3);
        attendant.setPraiseRate(67);

        when(userService.findById(21)).thenReturn(user);
        when(attendantService.findByUserId(21)).thenReturn(attendant);

        UserAttendantController controller = new UserAttendantController(attendantService, userService);
        ResponseResult<Map<String, Object>> response = controller.getAttendantDetail(21);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).containsEntry("score", 4.0D);
        assertThat(response.getData()).containsEntry("evaluationCount", 3);
        assertThat(response.getData()).containsEntry("praiseRate", 67);
    }
}
