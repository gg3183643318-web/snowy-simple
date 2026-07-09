package com.example.snowydemo.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.snowydemo")
public class SimpleSnowyDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleSnowyDemoApplication.class, args);
    }
}
