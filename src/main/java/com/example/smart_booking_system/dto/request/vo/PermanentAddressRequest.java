package com.example.smart_booking_system.dto.request.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermanentAddressRequest {
    private String streetAddress;
    private String wardCode;
    private String wardName;
    private String districtCode;
    private String districtName;
    private String provinceCode;
    private String provinceName;
}
