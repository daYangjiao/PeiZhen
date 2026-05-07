package org.example.controller;

import org.example.common.ResponseResult;
import org.example.service.AttendantService;
import org.example.service.FileStorageService;
import org.example.service.OrderEvaluationService;
import org.example.service.OrderService;
import org.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendantControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private AttendantService attendantService;
    @Mock
    private UserService userService;
    @Mock
    private OrderEvaluationService evaluationService;
    @Mock
    private FileStorageService fileStorageService;

    @Test
    void endServiceShouldTreatPendingConfirmationMessageAsSuccess() {
        AttendantController controller = new AttendantController(
                orderService,
                attendantService,
                userService,
                evaluationService,
                fileStorageService
        );
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("currentUserId")).thenReturn(20);

        when(orderService.endService(110, 20, new BigDecimal("2.5"), "检查排队较久"))
                .thenReturn("服务已提交，待用户确认时长费用");

        ResponseResult<String> response = controller.endService(
                110,
                new BigDecimal("2.5"),
                "检查排队较久",
                request
        );

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo("服务已提交，待用户确认时长费用");
    }
}
