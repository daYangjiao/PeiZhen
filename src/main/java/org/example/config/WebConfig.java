package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射本地文件系统路径
        // 注意：file: 后面是绝对路径，确保路径以 / 结尾
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:D:/NSU/lab/houduan2026.02.10/peizhen/src/main/resources/static/uploads/");
    }
}
