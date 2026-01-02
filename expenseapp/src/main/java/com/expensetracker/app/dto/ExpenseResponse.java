package com.expensetracker.app.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.*;

@Getter // <-- CRITICAL: Adds public getter methods for Jackson
@AllArgsConstructor // Optiona
@NoArgsConstructor
public class ExpenseResponse {

    private Long id;
    private BigDecimal amount;
    private String description;
    private String paidBy;
    private OffsetDateTime date;
    private List<SplitDetailResponse> splits;

    // // --- Constructor ---
    // public ExpenseResponse(Long id, BigDecimal amount, String description, String paidBy, OffsetDateTime date, List<SplitDetailResponse> splits) {
    //     this.id = id;
    //     this.amount = amount;
    //     this.description = description;
    //     this.paidBy = paidBy;
    //     this.date = date;
    //     this.splits = splits;
    // }
    
    // --- Getters and Setters (Omitted for brevity) ---
    // ...

    // --- Nested DTO for Split Response ---
    @Getter
    public static class SplitDetailResponse {
        private String memberUsername;
        private BigDecimal owedAmount;
        private BigDecimal paidAmount;
        private BigDecimal netBalance; // Useful for client display (Paid - Owed)

        public SplitDetailResponse(String memberUsername, BigDecimal owedAmount, BigDecimal paidAmount, BigDecimal netBalance) {
            this.memberUsername = memberUsername;
            this.owedAmount = owedAmount;
            this.paidAmount = paidAmount;
            this.netBalance = netBalance;
        }

        // --- Getters and Setters (Omitted for brevity) ---
        // ...
    }
    
}