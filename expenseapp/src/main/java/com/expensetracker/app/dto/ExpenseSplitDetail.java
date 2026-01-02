package com.expensetracker.app.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@Getter
public class ExpenseSplitDetail {

    @NotBlank(message = "Member username is required for split detail.")
    private String memberUsername;

    @NotNull(message = "Owed amount is required.")
    @DecimalMin(value = "0.00", message = "Owed amount cannot be negative.")
    private BigDecimal owedAmount;

    @NotNull(message = "Paid amount is required.")
    @DecimalMin(value = "0.00", message = "Paid amount cannot be negative.")
    private BigDecimal paidAmount;

    // --- Constructors ---

    public ExpenseSplitDetail() {
    }

    // public ExpenseSplitDetail(String memberUsername, BigDecimal owedAmount, BigDecimal paidAmount) {
    //     this.memberUsername = memberUsername;
    //     this.owedAmount = owedAmount;
    //     this.paidAmount = paidAmount;
    // }

    // --- Getters and Setters ---

    public String getMemberUsername() {
        return memberUsername;
    }

    public void setMemberUsername(String memberUsername) {
        this.memberUsername = memberUsername;
    }

    public BigDecimal getOwedAmount() {
        return owedAmount;
    }

    public void setOwedAmount(BigDecimal owedAmount) {
        this.owedAmount = owedAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    // Optional: toString for logging
    @Override
    public String toString() {
        return "ExpenseSplitDetail{" +
               "memberUsername='" + memberUsername + '\'' +
               ", owedAmount=" + owedAmount +
               ", paidAmount=" + paidAmount +
               '}';
    }
}