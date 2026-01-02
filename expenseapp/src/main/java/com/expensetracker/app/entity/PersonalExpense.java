package com.expensetracker.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "personal_expenses")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PersonalExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExpenseCategory category;

    @Column(name = "total_spent", nullable = false)
    private Double totalSpent;

    @Column(name = "amount_saved", nullable = false)
    private Double amountSaved;

    @Column(name = "expense_date", nullable = false)
    private OffsetDateTime expenseDate;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_group_id")
    private PersonalExpenseGroup expenseGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Custom constructor for easy object creation
    public PersonalExpense(String title, String description, ExpenseCategory category, 
                          Double totalSpent, Double amountSaved, User user) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.totalSpent = totalSpent;
        this.amountSaved = amountSaved;
        this.expenseDate = OffsetDateTime.now();
        this.user = user;
    }

    // Utility method to get total budget (total spent + amount saved)
    public Double getTotalBudget() {
        return totalSpent + amountSaved;
    }

    // Utility method to get savings percentage
    public Double getSavingsPercentage() {
        Double totalBudget = getTotalBudget();
        if (totalBudget == null || totalBudget == 0) {
            return 0.0;
        }
        return (amountSaved / totalBudget) * 100;
    }

    // Utility method to check if any money was saved
    public boolean hasSavings() {
        return amountSaved != null && amountSaved > 0;
    }
}