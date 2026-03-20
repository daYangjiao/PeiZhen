package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.BindingResult;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.http.HttpServletRequest;

@Configuration
@EnableSwagger2
public class Swagger2Config {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("org.example.controller"))
                .paths(PathSelectors.any())
                .build()
                .enableUrlTemplating(false) // 禁用 URL 模板，减少解析复杂度
                .ignoredParameterTypes(Object.class, HttpServletRequest.class, BindingResult.class); // 隐藏框架参数
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("愈安伴系统API文档")
                .description("覆盖用户、订单、聊天、文件上传、AI导诊、AI医疗问答等模块的接口文档。响应统一使用 ResponseResult<T> 包裹。")
                .version("1.0")
                .contact(new Contact("开发团队", "", ""))
                .build();
    }
}
