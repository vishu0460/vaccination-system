package com.vaccine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VaccinationApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VaccinationApplication.class);
        application.run(args);
    }
}
