package com.example.medicalsearch;

import com.example.medicalsearch.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
@SpringBootApplication
public class MedicalSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalSearchApplication.class, args);
    }
}
