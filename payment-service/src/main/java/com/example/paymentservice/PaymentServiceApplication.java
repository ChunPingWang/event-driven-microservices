package com.example.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;

@SpringBootApplication
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PaymentServiceApplication.class);
        
        // 默認使用開發環境配置
        app.setDefaultProperties(Collections.singletonMap("spring.profiles.default", "dev"));
        
        app.run(args);
    }
}