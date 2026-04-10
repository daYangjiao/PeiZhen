package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("org.example.dao") // 仅扫描MyBatis的Mapper包
@EnableScheduling
public class DbTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(DbTestApplication.class, args);
    }
}