package com.expensetracker.app.controller;

import com.expensetracker.app.dto.*;
import com.expensetracker.app.entity.PersonalExpense;
import com.expensetracker.app.service.PersonalExpenseService;
import com.expensetracker.app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class PersonalExpenseController {

    private final PersonalExpenseService expenseService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllExpenses(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.findUserIdByUsername(username);
            List<PersonalExpenseDTO> expenses = expenseService.getAllExpensesByUserId(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Expenses retrieved successfully", expenses));
        } catch (Exception e) {
            log.error("Error getting all expenses: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse> getExpenseById(
            @PathVariable Long expenseId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.findUserIdByUsername(username);
            PersonalExpenseDTO expense = expenseService.getExpenseById(expenseId, userId);
            return ResponseEntity.ok(new ApiResponse(true, "Expense retrieved successfully", expense));
        } catch (Exception e) {
            log.error("Error getting expense by ID: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createExpense(
            @RequestBody CreatePersonalExpenseRequestDTO request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Creating expense for user: {}", username);
            
            Long userId = userService.findUserIdByUsername(username);
            log.info("Found user ID: {} for username: {}", userId, username);

            PersonalExpense expense = PersonalExpense.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .category(request.getCategory())
                    .totalSpent(request.getTotalSpent())
                    .amountSaved(request.getAmountSaved())
                    .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : OffsetDateTime.now())
                    .paymentMethod(request.getPaymentMethod())
                    .expenseGroup(
                            request.getExpenseGroupId() != null
                                    ? com.expensetracker.app.entity.PersonalExpenseGroup.builder()
                                            .id(request.getExpenseGroupId()).build()
                                    : null)
                    .build();

            log.info("Expense object created: {}", expense);
            
            PersonalExpenseDTO savedExpense = expenseService.createExpense(expense, userId);
            log.info("Expense saved successfully with ID: {}", savedExpense.getId());
            
            return ResponseEntity.ok(new ApiResponse(true, "Expense created successfully", savedExpense));
        } catch (Exception e) {
            log.error("Error creating expense: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ApiResponse> updateExpense(
            @PathVariable Long expenseId,
            @RequestBody UpdatePersonalExpenseRequestDTO request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.findUserIdByUsername(username);

            PersonalExpense updatedExpense = PersonalExpense.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .category(request.getCategory())
                    .totalSpent(request.getTotalSpent())
                    .amountSaved(request.getAmountSaved())
                    .expenseDate(request.getExpenseDate())
                    .paymentMethod(request.getPaymentMethod())
                    .expenseGroup(
                            request.getExpenseGroupId() != null
                                    ? com.expensetracker.app.entity.PersonalExpenseGroup.builder()
                                            .id(request.getExpenseGroupId()).build()
                                    : null)
                    .build();

            PersonalExpenseDTO result = expenseService.updateExpense(expenseId, updatedExpense, userId);
            return ResponseEntity.ok(new ApiResponse(true, "Expense updated successfully", result));
        } catch (Exception e) {
            log.error("Error updating expense: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse> deleteExpense(
            @PathVariable Long expenseId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.findUserIdByUsername(username);
            expenseService.deleteExpense(expenseId, userId);
            return ResponseEntity.ok(new ApiResponse(true, "Expense deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting expense: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse> getExpensesByCategory(
            @PathVariable String category,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.findUserIdByUsername(username);
            com.expensetracker.app.entity.ExpenseCategory expenseCategory = com.expensetracker.app.entity.ExpenseCategory
                    .valueOf(category.toUpperCase());
            List<PersonalExpenseDTO> expenses = expenseService.getExpensesByCategory(userId, expenseCategory);
            return ResponseEntity.ok(new ApiResponse(true, "Expenses retrieved by category", expenses));
        } catch (Exception e) {
            log.error("Error getting expenses by category: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse> getExpensesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.findUserIdByUsername(username);
            List<PersonalExpenseDTO> expenses = expenseService.getExpensesByDateRange(userId, startDate, endDate);
            return ResponseEntity.ok(new ApiResponse(true, "Expenses retrieved by date range", expenses));
        } catch (Exception e) {
            log.error("Error getting expenses by date range: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse> getExpensesByGroupId(
            @PathVariable Long groupId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.findUserIdByUsername(username);
            List<PersonalExpenseDTO> expenses = expenseService.getExpensesByGroupId(groupId, userId);
            return ResponseEntity.ok(new ApiResponse(true, "Expenses retrieved by group", expenses));
        } catch (Exception e) {
            log.error("Error getting expenses by group: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchExpenses(
            @RequestParam String keyword,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.findUserIdByUsername(username);
            List<PersonalExpenseDTO> expenses = expenseService.searchExpenses(userId, keyword);
            return ResponseEntity.ok(new ApiResponse(true, "Expenses search completed", expenses));
        } catch (Exception e) {
            log.error("Error searching expenses: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse> getRecentExpenses(Authentication authentication) {
        try {
            String username = authentication.getName();
            Long userId = userService.findUserIdByUsername(username);
            List<PersonalExpenseDTO> expenses = expenseService.getRecentExpenses(userId, 5);
            return ResponseEntity.ok(new ApiResponse(true, "Recent expenses retrieved", expenses));
        } catch (Exception e) {
            log.error("Error getting recent expenses: ", e);
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }
}