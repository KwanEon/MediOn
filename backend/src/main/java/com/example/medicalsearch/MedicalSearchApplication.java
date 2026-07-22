package com.example.medicalsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MedicalSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalSearchApplication.class, args);
    }
}
