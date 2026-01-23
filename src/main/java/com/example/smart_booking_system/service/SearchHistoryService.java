package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.SearchHistory;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.SearchHistoryRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    /**
     * Xử lý INSERT (Lưu lịch sử tìm kiếm)
     * Dùng @Async để chạy bất đồng bộ, không làm chậm request search chính
     */
    @Async
    @Transactional
    public void saveSearchHistory(String userId, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng cho lịch sử"));

        SearchHistory history = new SearchHistory(user, searchTerm.trim());
        searchHistoryRepository.save(history);
    }

    /**
     * Xử lý SELECT (Lấy lịch sử tìm kiếm)
     */
    @Transactional(readOnly = true)
    public List<SearchHistory> getSearchHistoryByUserId(String userId) {
        List<SearchHistory> historyList = searchHistoryRepository
                .findByUserUserIdOrderByCreatedAtDesc(userId);

        if (historyList.size() > 20) {
            return historyList.subList(0, 20);
        }
        return historyList;
    }
}