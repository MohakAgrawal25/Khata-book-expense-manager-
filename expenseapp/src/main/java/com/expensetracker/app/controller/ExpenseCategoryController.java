package com.expensetracker.app.controller;

import com.expensetracker.app.dto.ApiResponse;
import com.expensetracker.app.dto.ExpenseCategoryDTO;
import com.expensetracker.app.entity.ExpenseCategory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class ExpenseCategoryController {

    @GetMapping
    public ResponseEntity<ApiResponse> getAllCategories() {
        try {
            List<ExpenseCategoryDTO> categories = Arrays.stream(ExpenseCategory.values())
                    .map(category -> new ExpenseCategoryDTO(
                            category.name(),
                            category.getDisplayName(),
                            category.getType().name()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse(true, "Categories retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }
}