package com.expensetracker.app.controller;

import com.expensetracker.app.dto.*;
import com.expensetracker.app.entity.PersonalExpenseGroup;
import com.expensetracker.app.service.PersonalExpenseGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/expense-groups")
@RequiredArgsConstructor
public class PersonalExpenseGroupController {

    private final PersonalExpenseGroupService expenseGroupService;

    // Create a new expense group
    @PostMapping
    public ResponseEntity<?> createExpenseGroup(
            @RequestBody CreatePersonalExpenseGroupRequestDTO request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            PersonalExpenseGroupDTO response = expenseGroupService.createGroup(convertToEntity(request), username);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get all expense groups for logged-in user
    @GetMapping
    public ResponseEntity<?> getUserExpenseGroups(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<PersonalExpenseGroupDTO> groups = expenseGroupService.getUserGroups(username);
            return ResponseEntity.ok(groups);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get specific expense group details
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getExpenseGroupById(
            @PathVariable Long groupId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            PersonalExpenseGroupDTO response = expenseGroupService.getGroupById(groupId, username);
            if (response == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense group not found or access denied.");
            }
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Update expense group
    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateExpenseGroup(
            @PathVariable Long groupId,
            @RequestBody UpdatePersonalExpenseGroupRequestDTO request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            PersonalExpenseGroupDTO response = expenseGroupService.updateGroup(groupId, convertToEntity(request), username);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Delete expense group
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteExpenseGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            expenseGroupService.deleteGroup(groupId, username);
            return ResponseEntity.ok("Expense group deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Search expense groups by title
    @GetMapping("/search")
    public ResponseEntity<?> searchExpenseGroups(
            @RequestParam String title,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            List<PersonalExpenseGroupDTO> groups = expenseGroupService.searchGroupsByTitle(username, title);
            return ResponseEntity.ok(groups);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get expense groups by date range
    @GetMapping("/date-range")
    public ResponseEntity<?> getExpenseGroupsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            List<PersonalExpenseGroupDTO> groups = expenseGroupService.getGroupsByDateRange(username, startDate, endDate);
            return ResponseEntity.ok(groups);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Helper method to convert DTO to Entity
    private PersonalExpenseGroup convertToEntity(CreatePersonalExpenseGroupRequestDTO request) {
        return PersonalExpenseGroup.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .build();
    }

    // Helper method to convert Update DTO to Entity
    private PersonalExpenseGroup convertToEntity(UpdatePersonalExpenseGroupRequestDTO request) {
        return PersonalExpenseGroup.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .build();
    }
}