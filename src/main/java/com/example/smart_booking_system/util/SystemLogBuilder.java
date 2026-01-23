package com.example.smart_booking_system.util;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Room;

import java.math.BigDecimal;

public class SystemLogBuilder {

    public static String propertyCreated(Property p) {
        return "Tạo cơ sở lưu trú: " + p.getPropertyName();
    }

    public static String propertyStatusChanged(Property p, String from, String to) {
        return "Cập nhật trạng thái cơ sở lưu trú: "
                + p.getPropertyName()
                + " (" + from + " → " + to + ")";
    }

    public static String roomPriceUpdated(Room r, BigDecimal oldPrice, BigDecimal newPrice) {
        return "Cập nhật giá phòng "
                + r.getRoomName()
                + " (" + oldPrice + " → " + newPrice + ")";
    }
}
