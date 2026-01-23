package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.entity.SearchHistory;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/searchhistory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER') or hasRole('OWNER') or hasRole('ADMIN')")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    /**
     * API SELECT: Lấy lịch sử tìm kiếm của cá nhân
     */
    @GetMapping("/search")
    public ResponseEntity<?> getMySearchHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Yêu cầu đăng nhập");
        }

        String userId = userDetails.getUserId();
        List<SearchHistory> history = searchHistoryService.getSearchHistoryByUserId(userId);

        List<String> searchTerms = history.stream()
                .map(SearchHistory::getSearchTerm)
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(searchTerms);
    }
}