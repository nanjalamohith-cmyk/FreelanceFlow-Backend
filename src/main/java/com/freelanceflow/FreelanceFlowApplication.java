package com.freelanceflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FreelanceFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreelanceFlowApplication.class, args);
    }
}
