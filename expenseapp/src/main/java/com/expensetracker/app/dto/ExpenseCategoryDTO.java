package com.expensetracker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryDTO {
    private String value;
    private String displayName;
    private String type; // "ESSENTIAL", "LIFESTYLE", "FINANCIAL", etc.
}