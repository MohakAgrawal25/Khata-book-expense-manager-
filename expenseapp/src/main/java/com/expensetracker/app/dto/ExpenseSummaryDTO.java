package com.expensetracker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryDTO {
    private Double totalSpent;
    private Double totalSaved;
    private Double totalBudget;
    private Double overallSavingsPercentage;
    private Integer totalExpenses;
    private Integer totalGroups;
}