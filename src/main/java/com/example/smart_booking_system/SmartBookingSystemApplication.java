package com.example.smart_booking_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // ✅ Bật tính năng lập lịch
@EnableAsync // ✅ Bật tính năng xử lý bất đồng bộ (Async)
public class SmartBookingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartBookingSystemApplication.class, args);
    }
}