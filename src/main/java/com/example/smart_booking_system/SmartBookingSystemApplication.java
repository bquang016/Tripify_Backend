package com.example.smart_booking_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // ✅ Bật tính năng lập lịch
public class SmartBookingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartBookingSystemApplication.class, args);
    }
}