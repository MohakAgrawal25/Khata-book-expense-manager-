package com.expensetracker.app.repository.impl;

import com.expensetracker.app.dto.CategoryWiseExpenseDTO;
import com.expensetracker.app.entity.PersonalExpense;
import com.expensetracker.app.repository.PersonalExpenseRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class PersonalExpenseRepositoryCustomImpl implements PersonalExpenseRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<CategoryWiseExpenseDTO> getCategoryWiseExpenseSummary(Long userId) {
        String jpql = """
            SELECT NEW com.expensetracker.app.dto.CategoryWiseExpenseDTO(
                pe.category,
                pe.category.displayName,
                SUM(pe.totalSpent),
                SUM(pe.amountSaved),
                (SUM(pe.totalSpent) / (SELECT SUM(pe2.totalSpent) FROM PersonalExpense pe2 WHERE pe2.user.id = :userId)) * 100,
                COUNT(pe)
            )
            FROM PersonalExpense pe
            WHERE pe.user.id = :userId
            GROUP BY pe.category, pe.category.displayName
            ORDER BY SUM(pe.totalSpent) DESC
            """;
        
        TypedQuery<CategoryWiseExpenseDTO> query = entityManager.createQuery(jpql, CategoryWiseExpenseDTO.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @Override
    public List<CategoryWiseExpenseDTO> getCategoryWiseExpenseSummaryByDateRange(
            Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        String jpql = """
            SELECT NEW com.expensetracker.app.dto.CategoryWiseExpenseDTO(
                pe.category,
                pe.category.displayName,
                SUM(pe.totalSpent),
                SUM(pe.amountSaved),
                (SUM(pe.totalSpent) / (SELECT SUM(pe2.totalSpent) FROM PersonalExpense pe2 WHERE pe2.user.id = :userId AND pe2.expenseDate BETWEEN :startDate AND :endDate)) * 100,
                COUNT(pe)
            )
            FROM PersonalExpense pe
            WHERE pe.user.id = :userId AND pe.expenseDate BETWEEN :startDate AND :endDate
            GROUP BY pe.category, pe.category.displayName
            ORDER BY SUM(pe.totalSpent) DESC
            """;
        
        TypedQuery<CategoryWiseExpenseDTO> query = entityManager.createQuery(jpql, CategoryWiseExpenseDTO.class);
        query.setParameter("userId", userId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }

    @Override
    public List<PersonalExpense> findExpensesWithHighSavings(Long userId, Double savingsThreshold) {
        String jpql = """
            SELECT pe FROM PersonalExpense pe 
            WHERE pe.user.id = :userId 
            AND pe.amountSaved >= :savingsThreshold
            ORDER BY pe.amountSaved DESC
            """;
        
        TypedQuery<PersonalExpense> query = entityManager.createQuery(jpql, PersonalExpense.class);
        query.setParameter("userId", userId);
        query.setParameter("savingsThreshold", savingsThreshold);
        return query.getResultList();
    }
}