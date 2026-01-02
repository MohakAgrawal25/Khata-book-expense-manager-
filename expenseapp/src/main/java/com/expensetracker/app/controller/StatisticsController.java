package com.expensetracker.app.controller;

import com.expensetracker.app.dto.ApiResponse;
import com.expensetracker.app.dto.CategoryWiseExpenseDTO;
import com.expensetracker.app.dto.ExpenseSummaryDTO;
import com.expensetracker.app.service.ExpenseStatisticsService;
import com.expensetracker.app.service.PersonalExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final ExpenseStatisticsService statisticsService;
    private final PersonalExpenseService expenseService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse> getExpenseSummary(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromUserDetails(userDetails);
            ExpenseSummaryDTO summary = statisticsService.getExpenseSummary(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Expense summary retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/category-wise")
    public ResponseEntity<ApiResponse> getCategoryWiseExpenses(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromUserDetails(userDetails);
            List<CategoryWiseExpenseDTO> categoryWiseExpenses = expenseService.getCategoryWiseSummary(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Category-wise expenses retrieved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/summary/date-range")
    public ResponseEntity<ApiResponse> getExpenseSummaryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromUserDetails(userDetails);
            statisticsService.getExpenseSummaryByDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(new ApiResponse(true, "Expense summary by date range retrieved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/total-spent")
    public ResponseEntity<ApiResponse> getTotalSpent(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromUserDetails(userDetails);
            expenseService.getTotalSpentByUserId(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Total spent retrieved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/total-saved")
    public ResponseEntity<ApiResponse> getTotalSaved(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = getUserIdFromUserDetails(userDetails);
            expenseService.getTotalSavedByUserId(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Total saved retrieved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        if (userDetails instanceof com.expensetracker.app.entity.User) {
            return ((com.expensetracker.app.entity.User) userDetails).getId();
        }
        throw new RuntimeException("Unable to get user ID from authentication");
    }
}