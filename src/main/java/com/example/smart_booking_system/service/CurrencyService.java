package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.ExchangeRate;
import com.example.smart_booking_system.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final SystemSettingService systemSettingService;
    private final ExchangeRateRepository exchangeRateRepository;

    public BigDecimal convertFromVND(BigDecimal amountVND) {
        if (amountVND == null) return BigDecimal.ZERO;

        String targetCurrency = systemSettingService.getDefaultCurrency();
        if ("VND".equalsIgnoreCase(targetCurrency)) {
            return amountVND;
        }

        // e.g. targetCurrency is "USD", we look for "USD_VND" rate
        // Assuming the rate is how many VND per 1 USD (e.g., 25000)
        String pair = targetCurrency.toUpperCase() + "_VND";
        return exchangeRateRepository.findByPair(pair)
                .map(rate -> amountVND.divide(BigDecimal.valueOf(rate.getRate()), 2, RoundingMode.HALF_UP))
                .orElse(amountVND); // Fallback to VND if rate not found
    }

    public String getCurrentCurrencySymbol() {
        return systemSettingService.getDefaultCurrency();
    }
}
