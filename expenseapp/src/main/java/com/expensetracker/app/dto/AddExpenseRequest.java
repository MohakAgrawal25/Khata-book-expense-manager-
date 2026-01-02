package com.expensetracker.app.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AddExpenseRequest {

    @NotNull(message = "Total amount is required.")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than zero.")
    private BigDecimal amount;

    @NotBlank(message = "Description is required.")
    @Size(max = 255, message = "Description cannot exceed 255 characters.")
    private String description;

    @NotBlank(message = "Payer (paidBy) username is required.")
    private String paidBy; // The user who paid the total amount

    @NotNull(message = "Split details are required.")
    @Size(min = 1, message = "There must be at least one split detail.")
    @Valid // Ensure nested DTOs are also validated
    private List<ExpenseSplitDetail> splitDetails;

    // --- Constructors ---

    public AddExpenseRequest() {
    }

    public AddExpenseRequest(BigDecimal amount, String description, String paidBy, List<ExpenseSplitDetail> splitDetails) {
        this.amount = amount;
        this.description = description;
        this.paidBy = paidBy;
        this.splitDetails = splitDetails;
    }

    // --- Getters and Setters ---

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(String paidBy) {
        this.paidBy = paidBy;
    }

    public List<ExpenseSplitDetail> getSplitDetails() {
        return splitDetails;
    }

    public void setSplitDetails(List<ExpenseSplitDetail> splitDetails) {
        this.splitDetails = splitDetails;
    }

    // Optional: toString for logging
    @Override
    public String toString() {
        return "AddExpenseRequest{" +
               "amount=" + amount +
               ", description='" + description + '\'' +
               ", paidBy='" + paidBy + '\'' +
               ", splitDetails=" + splitDetails +
               '}';
    }
}