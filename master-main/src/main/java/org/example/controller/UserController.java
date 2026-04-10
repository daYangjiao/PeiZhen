package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ResponseResult;
import org.example.model.User;
import org.example.service.UserService;
import org.example.unity.JwtUtil;
import org.example.util.AuthUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Api(tags = "用户管理接口")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @ApiOperation("用户注册")
    public ResponseResult<Integer> register(@Valid @RequestBody User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseResult.error(bindingResult.getFieldError().getDefaultMessage());
        }
        try {
            int userId = userService.register(user);
            return ResponseResult.success(Integer.valueOf(userId));
        } catch (IllegalArgumentException e) {
            return ResponseResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return ResponseResult.error("注册失败");
        }
    }

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public ResponseResult<Map<String, Object>> login(@RequestBody User user) {
        try {
            User loggedUser = userService.login(user.getUsername(), user.getPassword());
            if (loggedUser != null) {
                String token = jwtUtil.generateToken(loggedUser.getId());
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", loggedUser.getId());
                userInfo.put("username", loggedUser.getUsername());
                userInfo.put("name", loggedUser.getName());
                userInfo.put("userType", loggedUser.getUserType());
                userInfo.put("phone", loggedUser.getPhone());
                userInfo.put("avatar", loggedUser.getAvatar());
                return ResponseResult.success(Map.of("token", token, "userInfo", userInfo));
            }
            return ResponseResult.error("用户名或密码错误");
        } catch (Exception e) {
            log.error("登录异常: {}", user.getUsername(), e);
            return ResponseResult.error("服务器内部错误");
        }
    }

    @GetMapping("/current")
    @ApiOperation("获取当前用户信息")
    public ResponseResult<User> getCurrentUser(HttpServletRequest request) {
        try {
            Integer currentUserId = AuthUtil.getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseResult.unauthorized("用户未登录");
            }
            User user = userService.findById(currentUserId);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }
            return ResponseResult.success(user);
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return ResponseResult.error("获取失败: " + e.getMessage());
        }
    }
    
    // 其他CRUD接口根据需要保留或移除
    @GetMapping
    @ApiOperation("查询所有用户")
    public ResponseResult<List<User>> getAllUsers() {
        return ResponseResult.success(userService.findAll());
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询用户")
    public ResponseResult<User> getUser(@PathVariable Integer id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseResult.error("用户不存在");
        }
        return ResponseResult.success(user);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新用户信息")
    public ResponseResult<Integer> updateUser(@PathVariable Integer id, @Valid @RequestBody User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseResult.error(bindingResult.getFieldError().getDefaultMessage());
        }
        user.setId(id);
        int rows = userService.update(user);
        return ResponseResult.success(Integer.valueOf(rows));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除用户")
    public ResponseResult<Void> deleteUser(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseResult.success(null);
    }
}