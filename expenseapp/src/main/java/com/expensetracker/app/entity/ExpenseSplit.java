package com.expensetracker.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_splits")
@Getter // Use explicit Getters
@Setter // Use explicit Setters
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to the parent Expense
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense; 

    // The username of the member involved in this split
    @Column(name = "member_username", nullable = false, length = 100)
    private String memberUsername; 

    // The amount this member is responsible for (their share of the expense)
    @Column(name = "owed_amount", nullable = false)
    private BigDecimal owedAmount; 

    // The amount this member contributed toward the expense (can be 0)
    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount; 
    
    // The net balance for this member for this specific expense
    // Calculated as: paidAmount - owedAmount
    // This column is optional, but often useful for quick queries.
    @Column(name = "net_balance", nullable = false)
    private BigDecimal netBalance; 

    // --- Constructors, Getters, and Setters omitted for brevity ---
}