package com.example.smart_booking_system.dto.request;

import lombok.Data;

@Data
public class SystemSettingUpdateDTO {
    private String appName;
    private String defaultLanguage;
    private String defaultCurrency;
}
