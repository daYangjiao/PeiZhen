package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 头像/图片：数据库存 /uploads/04_Medicalcompanion.jpg，访问 /uploads/xxx 直接映射到本机目录
        // 路径必须与 application.properties 里 app.upload-dir 一致，末尾加 /
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:D:/NSU/lab/2026.02.15houduan/src/main/resources/static/uploads/");
    }
}
