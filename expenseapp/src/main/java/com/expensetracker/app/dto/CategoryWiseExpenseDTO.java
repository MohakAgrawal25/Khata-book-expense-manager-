package com.expensetracker.app.dto;

import com.expensetracker.app.entity.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryWiseExpenseDTO {
    private ExpenseCategory category;
    private Double totalSpent;
    private Double totalSaved;
    private Double percentageOfTotal;
    private Integer expenseCount;

    // Updated constructor for JPQL query - removed displayName parameter
    public CategoryWiseExpenseDTO(ExpenseCategory category, Double totalSpent, 
                                 Double totalSaved, Double percentageOfTotal, Long expenseCount) {
        this.category = category;
        this.totalSpent = totalSpent != null ? totalSpent : 0.0;
        this.totalSaved = totalSaved != null ? totalSaved : 0.0;
        this.percentageOfTotal = percentageOfTotal != null ? percentageOfTotal : 0.0;
        this.expenseCount = expenseCount != null ? expenseCount.intValue() : 0;
    }

    // Add getter for displayName that uses the enum's method
    public String getDisplayName() {
        return category != null ? category.getDisplayName() : "";
    }

    // Add getter for type that uses the enum's method
    public String getType() {
        return category != null ? category.getType().name() : "";
    }
}