package com.expensetracker.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "expenses")
@Getter // Use explicit Getters
@Setter // Use explicit Setters
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to the parent Group
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group; 

    // Total cost of the expense
    @Column(nullable = false)
    private BigDecimal amount; 

    @Column(nullable = false, length = 255)
    private String description;

    // The username of the user who paid the ENTIRE amount initially
    @Column(name = "paid_by_username", nullable = false, length = 100)
    private String paidByUsername; 

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt; 

    // One Expense can have many split details
    // Cascade allows splits to be saved/deleted with the expense
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseSplit> splits; 

    // --- Constructors, Getters, and Setters omitted for brevity ---
}