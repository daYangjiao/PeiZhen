package org.example.config;

import org.example.interceptor.AuthInterceptor;
import org.example.interceptor.AdminAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** 已由 WebConfig 映射到 file:.../static/uploads/，与数据库存的 /uploads/xxx 一致，此处不再重复
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**", "/attendant/**") // 拦截API与陪诊师端请求
                .excludePathPatterns(
                    "/api/admin/**",
                    "/api/users/login",
                    "/api/users/register",
                    "/api/users/checkUsername",
                    "/api/users/wechat/config-status",
                    "/api/users/wechat/login",
                    "/api/users/wechat/bind-phone",
                    "/api/app-upgrade/check",
                    "/api/common/upload",
                    "/api/common/upload-image",
                    "/swagger-ui/**",
                    "/v2/api-docs",
                    "/webjars/**",
                    "/swagger-resources/**",
                    "/favicon.ico"
                );

        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
        
        // 为AI导诊接口添加白名单（允许未登录用户访问预约流程）
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/ai/guide/**")
                .excludePathPatterns(
                    "/ai/guide/appointments",           // 预约创建
                    "/ai/guide/attendants/match",       // 陪诊师匹配
                    "/ai/guide/orders",                 // 订单创建
                    "/ai/guide/orders/*/complete-info", // 订单完整信息（支付成功后）
                    "/ai/guide/orders/*",               // 订单详情
                    "/ai/guide/payments/status",        // 支付状态更新
                    "/ai/guide/test/create-order-with-appointment", // 测试订单创建
                    "/ai/guide/test/latest-appointment", // 获取最新预约信息
                    "/ai/guide/appointments/*"          // 预约详情查询
                );
    }
}
