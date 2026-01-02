package com.expensetracker.app.repository;

import com.expensetracker.app.dto.CategoryWiseExpenseDTO;
import com.expensetracker.app.entity.ExpenseCategory;
import com.expensetracker.app.entity.PersonalExpense;
import com.expensetracker.app.entity.PersonalExpenseGroup;
import com.expensetracker.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PersonalExpenseRepository extends JpaRepository<PersonalExpense, Long> {

    List<PersonalExpense> findByUser(User user);

    List<PersonalExpense> findByUserId(Long userId);

    List<PersonalExpense> findByExpenseGroup(PersonalExpenseGroup expenseGroup);

    List<PersonalExpense> findByExpenseGroupId(Long expenseGroupId);

    List<PersonalExpense> findByUserIdAndCategory(Long userId, ExpenseCategory category);

    List<PersonalExpense> findByUserIdAndExpenseDateBetween(
            Long userId, OffsetDateTime startDate, OffsetDateTime endDate);

    List<PersonalExpense> findByExpenseGroupIdAndExpenseDateBetween(
            Long expenseGroupId, OffsetDateTime startDate, OffsetDateTime endDate);

    List<PersonalExpense> findByUserIdAndPaymentMethod(Long userId, String paymentMethod);

    @Query("SELECT pe FROM PersonalExpense pe WHERE pe.user.id = :userId AND pe.title LIKE %:keyword%")
    List<PersonalExpense> findByUserIdAndTitleContaining(@Param("userId") Long userId,
            @Param("keyword") String keyword);

    @Query("SELECT pe FROM PersonalExpense pe WHERE pe.user.id = :userId AND pe.description LIKE %:keyword%")
    List<PersonalExpense> findByUserIdAndDescriptionContaining(@Param("userId") Long userId,
            @Param("keyword") String keyword);

    // Statistics Queries
    @Query("SELECT COALESCE(SUM(pe.totalSpent), 0) FROM PersonalExpense pe WHERE pe.user.id = :userId")
    Double getTotalSpentByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(pe.amountSaved), 0) FROM PersonalExpense pe WHERE pe.user.id = :userId")
    Double getTotalSavedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(pe) FROM PersonalExpense pe WHERE pe.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    // 🆕 FIXED: Category Wise Expense Summary - Remove displayName reference
    @Query("SELECT new com.expensetracker.app.dto.CategoryWiseExpenseDTO(" +
            "pe.category, " +
            "COALESCE(SUM(pe.totalSpent), 0), " +
            "COALESCE(SUM(pe.amountSaved), 0), " +
            "CASE WHEN (SELECT COALESCE(SUM(pe2.totalSpent), 0) FROM PersonalExpense pe2 WHERE pe2.user.id = :userId) > 0 " +
            "THEN (COALESCE(SUM(pe.totalSpent), 0) / (SELECT COALESCE(SUM(pe2.totalSpent), 0) FROM PersonalExpense pe2 WHERE pe2.user.id = :userId)) * 100 " +
            "ELSE 0 END, " +
            "COUNT(pe)) " +
            "FROM PersonalExpense pe " +
            "WHERE pe.user.id = :userId " +
            "GROUP BY pe.category " +
            "ORDER BY SUM(pe.totalSpent) DESC")
    List<CategoryWiseExpenseDTO> getCategoryWiseExpenseSummary(@Param("userId") Long userId);

    @Query("SELECT pe.category, SUM(pe.totalSpent), SUM(pe.amountSaved), COUNT(pe) " +
            "FROM PersonalExpense pe WHERE pe.user.id = :userId " +
            "GROUP BY pe.category")
    List<Object[]> getCategoryWiseExpenses(@Param("userId") Long userId);

    @Query("SELECT SUM(pe.totalSpent) FROM PersonalExpense pe WHERE pe.user.id = :userId AND pe.expenseDate BETWEEN :startDate AND :endDate")
    Double getTotalSpentByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT SUM(pe.totalSpent) FROM PersonalExpense pe WHERE pe.expenseGroup.id = :groupId")
    Double getTotalSpentByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT SUM(pe.amountSaved) FROM PersonalExpense pe WHERE pe.expenseGroup.id = :groupId")
    Double getTotalSavedByGroupId(@Param("groupId") Long groupId);

    // For dashboard - recent expenses (FIXED: Use First instead of Top with hardcoded limit)
    List<PersonalExpense> findFirst5ByUserIdOrderByExpenseDateDesc(Long userId);

    // For finding expenses with high savings
    @Query("SELECT pe FROM PersonalExpense pe WHERE pe.user.id = :userId AND pe.amountSaved > 0 ORDER BY pe.amountSaved DESC")
    List<PersonalExpense> findExpensesWithSavings(@Param("userId") Long userId);

    // Monthly statistics
    @Query("SELECT MONTH(pe.expenseDate), YEAR(pe.expenseDate), SUM(pe.totalSpent), SUM(pe.amountSaved) " +
            "FROM PersonalExpense pe WHERE pe.user.id = :userId " +
            "GROUP BY YEAR(pe.expenseDate), MONTH(pe.expenseDate) " +
            "ORDER BY YEAR(pe.expenseDate) DESC, MONTH(pe.expenseDate) DESC")
    List<Object[]> getMonthlyExpenseSummary(@Param("userId") Long userId);

    List<PersonalExpense> findByUserIdAndTitleContainingIgnoreCase(Long userId, String keyword);

    List<PersonalExpense> findByUserIdAndDescriptionContainingIgnoreCase(Long userId, String keyword);

    // 🆕 FIXED: Use @Query for dynamic limit
    @Query(value = "SELECT * FROM personal_expenses pe WHERE pe.user_id = :userId ORDER BY pe.expense_date DESC LIMIT :limit", nativeQuery = true)
    List<PersonalExpense> findRecentExpensesByUserIdWithLimit(@Param("userId") Long userId, @Param("limit") int limit);

    // 🆕 ALTERNATIVE FIX: Use Pageable for dynamic limit
    // List<PersonalExpense> findByUserIdOrderByExpenseDateDesc(Long userId, Pageable pageable);
}