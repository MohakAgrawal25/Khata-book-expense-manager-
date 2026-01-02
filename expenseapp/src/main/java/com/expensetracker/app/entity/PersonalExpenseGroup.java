package com.expensetracker.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "personal_expense_groups")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PersonalExpenseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime fromDate;

    @Column(name = "end_date", nullable = false)
    private OffsetDateTime toDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🆕 One-to-Many relationship with expenses (inverse side of Many-to-One)
    @OneToMany(mappedBy = "expenseGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PersonalExpense> expenses = new ArrayList<>();

    // Custom constructor without ID and timestamps for easy object creation
    public PersonalExpenseGroup(String title, String description, OffsetDateTime fromDate, OffsetDateTime toDate, User user) {
        this.title = title;
        this.description = description;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.user = user;
        this.expenses = new ArrayList<>();
    }

    // 🆕 Utility methods to manage expenses
    public void addExpense(PersonalExpense expense) {
        expenses.add(expense);
        expense.setExpenseGroup(this);
    }

    public void removeExpense(PersonalExpense expense) {
        expenses.remove(expense);
        expense.setExpenseGroup(null);
    }

    // 🆕 Utility method to calculate total spent in this group
    public Double getTotalSpentInGroup() {
        return expenses.stream()
                .mapToDouble(PersonalExpense::getTotalSpent)
                .sum();
    }

    // 🆕 Utility method to calculate total saved in this group
    public Double getTotalSavedInGroup() {
        return expenses.stream()
                .mapToDouble(PersonalExpense::getAmountSaved)
                .sum();
    }

    // 🆕 Utility method to calculate total budget for this group
    public Double getTotalBudgetForGroup() {
        return expenses.stream()
                .mapToDouble(PersonalExpense::getTotalBudget)
                .sum();
    }

    // 🆕 Utility method to get overall savings percentage for the group
    public Double getGroupSavingsPercentage() {
        Double totalSpent = getTotalSpentInGroup();
        Double totalBudget = getTotalBudgetForGroup();
        
        if (totalBudget == null || totalBudget == 0) {
            return 0.0;
        }
        return ((totalBudget - totalSpent) / totalBudget) * 100;
    }

    // 🆕 Utility method to get number of expenses in this group
    public int getExpenseCount() {
        return expenses.size();
    }
}