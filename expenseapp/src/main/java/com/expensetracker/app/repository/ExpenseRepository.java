package com.expensetracker.app.repository;

import com.expensetracker.app.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * JPA Repository for the Expense entity.
 * Provides basic CRUD operations and custom query methods.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Finds all expenses belonging to a specific group ID.
     * Uses the 'group' field in the Expense entity.
     * @param groupId The ID of the Group.
     * @return A list of Expenses in that group, ordered by creation date descending.
     */
    List<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    /**
     * Counts the total number of expenses for a specific group.
     * @param groupId The ID of the Group.
     * @return The count of expenses.
     */
    long countByGroupId(Long groupId);

    // In your ExpenseRepository.java - Add this method

/**
 * Finds an expense by ID and group ID with eager fetching of splits.
 * This ensures we get the expense only if it belongs to the specified group.
 */
@Query("SELECT e FROM Expense e LEFT JOIN FETCH e.splits WHERE e.id = :expenseId AND e.group.id = :groupId")
Optional<Expense> findByIdAndGroupIdWithSplits(@Param("expenseId") Long expenseId, @Param("groupId") Long groupId);

/**
 * Alternative method without eager fetching (if you prefer to handle it in service)
 */
Optional<Expense> findByIdAndGroupId(Long id, Long groupId);
}