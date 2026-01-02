package com.expensetracker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalExpenseGroupDTO {
    private Long id;
    private String title;
    private String description;
    private OffsetDateTime fromDate;
    private OffsetDateTime toDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long userId;
    private String username;
    private int expenseCount;
    private Double totalSpentInGroup;
    private Double totalSavedInGroup;
    private Double totalBudgetForGroup;
    private Double groupSavingsPercentage;
}