package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.ExchangeRateUpdateDTO;
import com.example.smart_booking_system.dto.request.MaintenanceModeUpdateDTO;
import com.example.smart_booking_system.dto.request.SystemSettingUpdateDTO;
import com.example.smart_booking_system.dto.response.ExchangeRateResponseDTO;
import com.example.smart_booking_system.dto.response.SystemSettingResponseDTO;
import com.example.smart_booking_system.entity.ExchangeRate;
import com.example.smart_booking_system.entity.SystemSetting;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.LogAction;
import com.example.smart_booking_system.enums.LogEntityType;
import com.example.smart_booking_system.repository.ExchangeRateRepository;
import com.example.smart_booking_system.repository.SystemSettingRepository;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public SystemSettingResponseDTO getSettings() {
        SystemSetting settings = systemSettingRepository.findById(1L)
                .orElseGet(() -> systemSettingRepository.save(new SystemSetting()));
        return toResponseDTO(settings);
    }

    @Transactional
    public SystemSettingResponseDTO updateSettings(SystemSettingUpdateDTO updateDTO) {
        SystemSetting settings = systemSettingRepository.findById(1L)
                .orElseGet(() -> new SystemSetting());
        
        String oldVal = settings.toString();

        if (updateDTO.getAppName() != null) settings.setAppName(updateDTO.getAppName());
        if (updateDTO.getDefaultLanguage() != null) settings.setDefaultLanguage(updateDTO.getDefaultLanguage());
        if (updateDTO.getDefaultCurrency() != null) settings.setDefaultCurrency(updateDTO.getDefaultCurrency());
        
        SystemSetting saved = systemSettingRepository.save(settings);
        
        systemLogService.log(
                getCurrentUser(),
                LogAction.UPDATE,
                LogEntityType.SYSTEM_SETTING,
                "1",
                "Cập nhật cấu hình hệ thống",
                oldVal,
                saved.toString()
        );

        return toResponseDTO(saved);
    }

    @Transactional
    public void updateLogo(String logoUrl) {
        SystemSetting settings = systemSettingRepository.findById(1L)
                .orElseGet(() -> new SystemSetting());
        String oldLogo = settings.getLogoUrl();
        settings.setLogoUrl(logoUrl);
        systemSettingRepository.save(settings);

        systemLogService.log(
                getCurrentUser(),
                LogAction.UPDATE,
                LogEntityType.SYSTEM_SETTING,
                "1",
                "Cập nhật Logo hệ thống",
                oldLogo,
                logoUrl
        );
    }

    @Transactional
    public void deleteLogo() {
        SystemSetting settings = systemSettingRepository.findById(1L)
                .orElseGet(() -> new SystemSetting());
        String oldLogo = settings.getLogoUrl();
        settings.setLogoUrl(null);
        systemSettingRepository.save(settings);

        systemLogService.log(
                getCurrentUser(),
                LogAction.DELETE,
                LogEntityType.SYSTEM_SETTING,
                "1",
                "Xóa Logo hệ thống",
                oldLogo,
                null
        );
    }

    public List<ExchangeRateResponseDTO> getExchangeRates() {
        return exchangeRateRepository.findAll().stream()
                .map(this::toExchangeRateDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExchangeRateResponseDTO updateExchangeRate(ExchangeRateUpdateDTO updateDTO) {
        ExchangeRate rate = exchangeRateRepository.findByPair(updateDTO.getPair())
                .orElseGet(() -> {
                    ExchangeRate newRate = new ExchangeRate();
                    newRate.setPair(updateDTO.getPair());
                    return newRate;
                });
        
        String oldVal = rate.getRate() != null ? rate.getRate().toString() : "0";
        rate.setRate(updateDTO.getRate());
        ExchangeRate saved = exchangeRateRepository.save(rate);

        systemLogService.log(
                getCurrentUser(),
                LogAction.UPDATE,
                LogEntityType.EXCHANGE_RATE,
                saved.getId().toString(),
                "Cập nhật tỷ giá: " + saved.getPair(),
                oldVal,
                saved.getRate().toString()
        );

        return toExchangeRateDTO(saved);
    }

    @Transactional
    public void updateMaintenanceMode(MaintenanceModeUpdateDTO updateDTO) {
        SystemSetting settings = systemSettingRepository.findById(1L)
                .orElseGet(() -> new SystemSetting());
        
        boolean oldMode = settings.getIsMaintenanceMode();
        settings.setIsMaintenanceMode(updateDTO.getIsEnabled());
        settings.setMaintenanceMessage(updateDTO.getMessage());
        systemSettingRepository.save(settings);

        systemLogService.log(
                getCurrentUser(),
                LogAction.UPDATE,
                LogEntityType.SYSTEM_SETTING,
                "1",
                "Cập nhật chế độ bảo trì: " + (updateDTO.getIsEnabled() ? "BẬT" : "TẮT"),
                String.valueOf(oldMode),
                String.valueOf(updateDTO.getIsEnabled())
        );
    }

    private SystemSettingResponseDTO toResponseDTO(SystemSetting settings) {
        return new SystemSettingResponseDTO(
                settings.getAppName(),
                settings.getDefaultLanguage(),
                settings.getDefaultCurrency(),
                settings.getLogoUrl(),
                settings.getIsMaintenanceMode(),
                settings.getMaintenanceMessage()
        );
    }

    private ExchangeRateResponseDTO toExchangeRateDTO(ExchangeRate rate) {
        return new ExchangeRateResponseDTO(rate.getPair(), rate.getRate());
    }
}
