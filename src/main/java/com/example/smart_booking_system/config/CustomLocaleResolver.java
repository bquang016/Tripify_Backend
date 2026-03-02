package com.example.smart_booking_system.config;

import com.example.smart_booking_system.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AbstractLocaleResolver;

import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class CustomLocaleResolver extends AbstractLocaleResolver {

    private final SystemSettingService systemSettingService;

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String language = systemSettingService.getDefaultLanguage();
        return language != null ? new Locale(language) : Locale.getDefault();
    }

    @Override
    public void setLocale(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Locale locale) {
        // We don't support setting locale per request via this resolver as it follows system settings
    }
}
