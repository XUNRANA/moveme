package com.moveme;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.moveme.module.*.mapper")
@EnableScheduling
public class MovemeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovemeApplication.class, args);
    }
}
