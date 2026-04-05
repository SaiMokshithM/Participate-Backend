package com.participatehere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
public class ParticipateHereApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParticipateHereApplication.class, args);
        System.out.println("Project Running Successfully");
    }
}
