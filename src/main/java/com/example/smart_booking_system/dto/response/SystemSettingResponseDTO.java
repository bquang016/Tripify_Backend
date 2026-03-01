package com.example.smart_booking_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingResponseDTO {
    private String appName;
    private String defaultLanguage;
    private String defaultCurrency;
    private String logoUrl;
    private Boolean isMaintenanceMode;
    private String maintenanceMessage;
}
