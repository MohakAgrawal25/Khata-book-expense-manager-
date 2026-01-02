package com.expensetracker.app.dto;

import com.expensetracker.app.entity.ExpenseCategory;
import com.expensetracker.app.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePersonalExpenseRequestDTO {
    private String title;
    private String description;
    private ExpenseCategory category;
    private Double totalSpent;
    private Double amountSaved;
    private OffsetDateTime expenseDate;
    private PaymentMethod paymentMethod;
    private Long expenseGroupId;
}