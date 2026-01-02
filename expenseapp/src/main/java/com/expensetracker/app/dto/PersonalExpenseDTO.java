package com.expensetracker.app.dto;

import com.expensetracker.app.entity.ExpenseCategory;
import com.expensetracker.app.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalExpenseDTO {
    private Long id;
    private String title;
    private String description;
    private ExpenseCategory category;
    private Double totalSpent;
    private Double amountSaved;
    private OffsetDateTime expenseDate;
    private PaymentMethod paymentMethod;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long expenseGroupId;
    private String expenseGroupTitle;
    private Long userId;
    private String username;
    
    // Calculated fields
    private Double totalBudget;
    private Double savingsPercentage;
    private Boolean hasSavings;
}