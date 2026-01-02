package com.expensetracker.app.controller;

import com.expensetracker.app.dto.AddExpenseRequest;
import com.expensetracker.app.dto.ExpenseResponse;
import com.expensetracker.app.service.ExpenseService;
import com.expensetracker.app.exception.ResourceNotFoundException;
import com.expensetracker.app.exception.ValidationException;
import com.expensetracker.app.entity.Expense;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;
import java.security.Principal;

import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // --- Endpoint 1: Add New Expense (POST) ---
    @PostMapping
    public ResponseEntity<?> addExpense(
            @PathVariable Long groupId,
            @Valid @RequestBody AddExpenseRequest request,
            Principal principal) {

        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication context is missing.");
        }

        String currentUsername = principal.getName();
        System.out.println("DEBUG: Current User (from Principal): " + currentUsername);
        
        try {
            Expense createdExpense = expenseService.createExpense(groupId, request, currentUsername);
            ExpenseResponse response = convertToExpenseResponse(createdExpense);
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal error occurred while processing the expense: " + e.getMessage());
        }
    }

    // --- Endpoint 2: Get All Expenses for a Group (GET) ---
    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getGroupExpenses(
            @PathVariable Long groupId,
            Principal principal) {
        
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        
        String currentUsername = principal.getName();
        try {
            List<Expense> expenses = expenseService.getExpensesByGroupId(groupId);
            List<ExpenseResponse> responseList = expenses.stream()
                    .map(this::convertToExpenseResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responseList);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // --- Endpoint 3: Update Existing Expense (PUT) ---
    @PutMapping("/{expenseId}")
    public ResponseEntity<?> updateExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @Valid @RequestBody AddExpenseRequest request,
            Principal principal) {

        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication context is missing.");
        }

        String currentUsername = principal.getName();
        System.out.println("DEBUG: Updating expense - Current User: " + currentUsername);

        try {
            Expense updatedExpense = expenseService.updateExpense(groupId, expenseId, request, currentUsername);
            ExpenseResponse response = convertToExpenseResponse(updatedExpense);
            return ResponseEntity.ok(response);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal error occurred while updating the expense: " + e.getMessage());
        }
    }

    // --- Endpoint 4: Get Single Expense by ID (GET) ---
    @GetMapping("/{expenseId}")
    public ResponseEntity<?> getExpenseById(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            Principal principal) {

        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User authentication context is missing.");
        }

        String currentUsername = principal.getName();
        System.out.println("DEBUG: Fetching single expense - Group ID: " + groupId + ", Expense ID: " + expenseId);
        System.out.println("DEBUG: Current User: " + currentUsername);

        try {
            Expense expense = expenseService.getExpenseById(groupId, expenseId, currentUsername);
            
            // Force initialization of splits to avoid LazyInitializationException
            if (expense.getSplits() != null) {
                expense.getSplits().size(); // This forces Hibernate to load the collection
                System.out.println("DEBUG: Controller - Initialized " + expense.getSplits().size() + " splits");
            }
            
            ExpenseResponse response = convertToExpenseResponse(expense);
            return ResponseEntity.ok(response);

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error fetching expense: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal error occurred while fetching the expense: " + e.getMessage());
        }
    }

    /**
     * Helper method to convert the JPA Entity to the API Response DTO.
     */
    private ExpenseResponse convertToExpenseResponse(Expense expense) {
        List<ExpenseResponse.SplitDetailResponse> splitDetails = expense.getSplits().stream()
                .map(split -> new ExpenseResponse.SplitDetailResponse(
                        split.getMemberUsername(),
                        split.getOwedAmount(),
                        split.getPaidAmount(),
                        split.getNetBalance()))
                .collect(Collectors.toList());

        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getPaidByUsername(),
                expense.getCreatedAt(),
                splitDetails);
    }
}