package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
   public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 头像/图片：数据库存 /uploads/xxx，映射到 classpath:/static/uploads/
        // Spring Boot 会自动处理 src/main/resources/static -> target/classes/static
       registry.addResourceHandler("/uploads/**")
                .addResourceLocations("classpath:/static/uploads/");
    }
}
