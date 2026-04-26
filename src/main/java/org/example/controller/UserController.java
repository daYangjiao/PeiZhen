package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ResponseResult;
import org.example.model.Attendant;
import org.example.model.User;
import org.example.model.request.WechatBindPhoneRequest;
import org.example.model.request.WechatLoginRequest;
import org.example.service.AttendantService;
import org.example.service.UserService;
import org.example.service.WechatAuthService;
import org.example.unity.JwtUtil;
import org.example.util.AuthUtil;
import org.springframework.validation.BindingResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/users")
@Api(tags = "用户管理接口")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final UserService userService;
    private final AttendantService attendantService;
    private final WechatAuthService wechatAuthService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "注册普通用户或陪诊师账号。请求体需要提供 phone、password、name、userType 等基础字段。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "注册成功，data 为新用户 ID"),
            @ApiResponse(code = 400, message = "参数校验失败或手机号已存在"),
            @ApiResponse(code = 500, message = "注册失败")
    })
    public ResponseResult<Integer> register(
            @ApiParam(value = "用户注册请求体", required = true)
            @Valid @RequestBody User user,
            @ApiIgnore BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseResult.error(bindingResult.getFieldError().getDefaultMessage());
        }
        String validationMessage = validateRegisterRequest(user);
        if (validationMessage != null) {
            return ResponseResult.error(validationMessage);
        }
        try {
            int userId;
            if (user.getUserType() != null && user.getUserType() == 1) {
                Attendant attendant = new Attendant();
                attendant.setCertificate(user.getCertificate());
                attendant.setIntroduction(user.getIntroduction());
                attendant.setProfessionalField(user.getProfessionalField());
                attendant.setExperienceYears(user.getExperienceYears() == null ? 0 : user.getExperienceYears());
                attendant.setHospitalName(user.getHospitalName());
                attendant.setStatus(0);
                userId = attendantService.registerAttendant(user, attendant);
            } else {
                userId = userService.register(user);
            }
            return ResponseResult.success(Integer.valueOf(userId));
        } catch (IllegalArgumentException e) {
            return ResponseResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return ResponseResult.error("注册失败");
        }
    }

    private String validateRegisterRequest(User user) {
        if (user == null) {
            return "注册信息不能为空";
        }
        if (user.getPhone() == null || !PHONE_PATTERN.matcher(user.getPhone().trim()).matches()) {
            return "请输入正确的手机号";
        }
        if (user.getName() == null || user.getName().trim().length() < 2) {
            return "姓名至少输入2个字";
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return "密码至少输入6位";
        }
        if (user.getSex() == null || user.getSex().trim().isEmpty() || "未知".equals(user.getSex().trim())) {
            return "请选择性别";
        }
        if (user.getAge() == null || user.getAge() <= 0 || user.getAge() > 120) {
            return "请输入正确的年龄";
        }
        if (user.getUserType() != null && user.getUserType() == 1) {
            if (user.getProfessionalField() == null || user.getProfessionalField().trim().length() < 2) {
                return "请填写擅长领域";
            }
            if (user.getIntroduction() == null || user.getIntroduction().trim().length() < 10) {
                return "个人简介至少输入10个字";
            }
            if (user.getHospitalName() == null || user.getHospitalName().trim().length() < 2) {
                return "请填写常驻医院";
            }
            if (user.getExperienceYears() == null || user.getExperienceYears() < 0 || user.getExperienceYears() > 60) {
                return "请输入正确的从业年限";
            }
        }
        return null;
    }

    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "使用手机号和密码登录。成功后返回 token 及 userInfo，后续受保护接口需通过请求头携带 token。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "登录成功，返回 token 和用户信息"),
            @ApiResponse(code = 400, message = "手机号或密码错误"),
            @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public ResponseResult<Map<String, Object>> login(
            @ApiParam(value = "登录请求体，仅需传入 phone 和 password", required = true)
            @RequestBody User user) {
        try {
            User loggedUser = userService.login(user.getPhone(), user.getPassword());
            if (loggedUser != null) {
                String token = jwtUtil.generateToken(loggedUser.getId());
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", loggedUser.getId());
                userInfo.put("name", loggedUser.getName());
                userInfo.put("userType", loggedUser.getUserType());
                userInfo.put("phone", loggedUser.getPhone());
                userInfo.put("avatar", loggedUser.getAvatar());
                return ResponseResult.success(Map.of("token", token, "userInfo", userInfo));
            }
            return ResponseResult.error("手机号或密码错误");
        } catch (Exception e) {
            log.error("登录异常: {}", user.getPhone(), e);
            return ResponseResult.error("服务器内部错误");
        }
    }

    @GetMapping("/wechat/config-status")
    @ApiOperation(value = "获取微信登录配置状态", notes = "前端用于判断微信登录按钮当前是否可真正启用。第一阶段仅小程序开放，支持 user 和 escort。")
    public ResponseResult<Map<String, Object>> getWechatConfigStatus(
            @ApiParam(value = "当前登录角色，支持 user 或 escort", example = "user")
            @RequestParam(value = "role", required = false) String role) {
        return ResponseResult.success(wechatAuthService.getConfigStatus(role));
    }

    @GetMapping("/wechat/oauth-url")
    @ApiOperation(value = "生成微信网页授权地址", notes = "H5 微信内网页登录使用，返回可跳转的微信授权 URL。")
    public ResponseResult<Map<String, Object>> getWechatOAuthUrl(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "platform", required = false, defaultValue = "WECHAT_H5") String platform,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl) {
        try {
            return ResponseResult.success(Map.of("url", wechatAuthService.buildUserOAuthUrl(role, platform, redirectUrl)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new ResponseResult<>(400, e.getMessage(), null);
        }
    }

    @GetMapping("/wechat/oauth-callback")
    @ApiOperation(value = "微信网页授权回调", notes = "微信回调后换取 openid/unionid，并跳回 H5 登录页。")
    public ResponseEntity<Void> handleWechatOAuthCallback(
            @RequestParam(value = "platform", required = false, defaultValue = "WECHAT_H5") String platform,
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {
        String redirectUrl = wechatAuthService.handleUserOAuthCallback(platform, code, state);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/wechat/login")
    @ApiOperation(value = "微信登录", notes = "使用小程序/App/H5 返回的 code 发起登录。若用户已绑定微信身份则直接返回 token；否则返回 wechatBindToken 进入手机号绑定。当前支持 user 和 escort。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "调用成功"),
            @ApiResponse(code = 400, message = "微信登录未开通、code 无效或当前角色暂不支持"),
            @ApiResponse(code = 500, message = "登录失败")
    })
    public ResponseResult<Map<String, Object>> wechatLogin(
            @ApiParam(value = "微信登录请求体", required = true)
            @RequestBody WechatLoginRequest request) {
        try {
            return ResponseResult.success(wechatAuthService.login(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new ResponseResult<>(400, e.getMessage(), null);
        } catch (Exception e) {
            log.error("微信登录失败", e);
            return ResponseResult.error("微信登录失败");
        }
    }

    @PostMapping("/wechat/bind-phone")
    @ApiOperation(value = "微信登录绑定手机号", notes = "微信登录未绑定手机号时，使用短期 wechatBindToken 携带 openid 完成手机号绑定，并直接返回登录态。当前支持 user 和 escort。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "绑定成功"),
            @ApiResponse(code = 400, message = "参数错误、凭证失效、手机号已绑定其他微信账号或当前角色不支持"),
            @ApiResponse(code = 500, message = "绑定失败")
    })
    public ResponseResult<Map<String, Object>> bindWechatPhone(
            @ApiParam(value = "微信绑定手机号请求体", required = true)
            @RequestBody WechatBindPhoneRequest request) {
        try {
            return ResponseResult.success(wechatAuthService.bindPhone(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new ResponseResult<>(400, e.getMessage(), null);
        } catch (Exception e) {
            log.error("微信绑定手机号失败", e);
            return ResponseResult.error("绑定失败");
        }
    }

    @GetMapping("/current")
    @ApiOperation(value = "获取当前用户信息", notes = "根据请求头中的 token 解析当前登录用户，并返回最新用户资料。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 401, message = "用户未登录或 token 无效"),
            @ApiResponse(code = 404, message = "用户不存在"),
            @ApiResponse(code = 500, message = "获取失败")
    })
    public ResponseResult<User> getCurrentUser(@ApiIgnore HttpServletRequest request) {
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

    @GetMapping
    @ApiOperation(value = "查询所有用户", notes = "后台调试接口，返回系统中的全部用户列表。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 500, message = "查询失败")
    })
    public ResponseResult<List<User>> getAllUsers() {
        return ResponseResult.success(userService.findAll());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID查询用户", notes = "按用户主键查询单个用户详情。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "查询成功"),
            @ApiResponse(code = 404, message = "用户不存在")
    })
    public ResponseResult<User> getUser(
            @ApiParam(value = "用户ID", required = true, example = "9")
            @PathVariable Integer id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseResult.error("用户不存在");
        }
        return ResponseResult.success(user);
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新用户信息", notes = "根据用户 ID 更新基础资料。请求体中的 id 会被路径参数覆盖。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "更新成功，data 为影响行数"),
            @ApiResponse(code = 400, message = "参数校验失败"),
            @ApiResponse(code = 500, message = "更新失败")
    })
    public ResponseResult<Integer> updateUser(
            @ApiParam(value = "用户ID", required = true, example = "9")
            @PathVariable Integer id,
            @ApiParam(value = "用户更新请求体", required = true)
            @Valid @RequestBody User user,
            @ApiIgnore BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseResult.error(bindingResult.getFieldError().getDefaultMessage());
        }
        try {
            user.setId(id);
            int rows = userService.update(user);
            return ResponseResult.success(Integer.valueOf(rows));
        } catch (IllegalArgumentException e) {
            return ResponseResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除用户", notes = "按用户 ID 删除指定用户。该接口偏后台管理用途。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "删除成功"),
            @ApiResponse(code = 404, message = "用户不存在"),
            @ApiResponse(code = 500, message = "删除失败")
    })
    public ResponseResult<Void> deleteUser(
            @ApiParam(value = "用户ID", required = true, example = "9")
            @PathVariable Integer id) {
        userService.delete(id);
        return ResponseResult.success(null);
    }
}
