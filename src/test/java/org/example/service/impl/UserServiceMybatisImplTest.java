package org.example.service.impl;

import org.example.dao.UserMapper;
import org.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceMybatisImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceMybatisImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceMybatisImpl();
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
    }

    @Test
    void loginShouldThrowBusinessMessageWhenAccountDisabled() {
        User user = new User();
        user.setPhone("13800000000");
        user.setStatus(0);
        user.setPassword("$2a$10$1234567890123456789012345678901234567890123456789012");
        when(userMapper.findByPhone("13800000000")).thenReturn(user);

        assertThatThrownBy(() -> service.login("13800000000", "123456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("账号已被禁用，请联系平台客服");
    }

    @Test
    void loginShouldStillReturnNullWhenPasswordWrong() {
        User user = new User();
        user.setPhone("13800000000");
        user.setStatus(1);
        user.setPassword("$2a$10$1234567890123456789012345678901234567890123456789012");
        when(userMapper.findByPhone("13800000000")).thenReturn(user);

        assertThat(service.login("13800000000", "bad")).isNull();
    }
}
