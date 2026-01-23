package com.example.smart_booking_system.util;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Room;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class SystemLogJsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    // =====================================================
    // PROPERTY SNAPSHOT
    // =====================================================
    public static String propertySnapshot(Property p) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("propertyId", p.getPropertyId());
            data.put("name", p.getPropertyName());
            data.put("status", p.getPropertyStatus());
            data.put("active", p.isActive());
            data.put("city", p.getCity());
            data.put("province", p.getProvince());
            data.put("updatedAt", p.getUpdatedAt());

            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    // =====================================================
    // ROOM SNAPSHOT (NEW)
    // =====================================================
    public static String roomSnapshot(Room r) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("roomId", r.getRoomId());
            data.put("propertyId", r.getProperty() != null ? r.getProperty().getPropertyId() : null);
            data.put("roomName", r.getRoomName());
            data.put("category", r.getRoomCategory());
            data.put("pricePerNight", r.getPricePerNight());
            data.put("weekendPrice", r.getWeekendPrice());
            data.put("capacity", r.getCapacity());
            data.put("status", r.getRoomStatus());
            data.put("active", r.isActive());

            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
