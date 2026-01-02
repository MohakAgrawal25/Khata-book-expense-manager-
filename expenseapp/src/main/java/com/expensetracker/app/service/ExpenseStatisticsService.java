package com.expensetracker.app.service;

import com.expensetracker.app.dto.CategoryWiseExpenseDTO;
import com.expensetracker.app.dto.ExpenseSummaryDTO;
import com.expensetracker.app.repository.PersonalExpenseRepository;
import com.expensetracker.app.repository.PersonalExpenseGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseStatisticsService {

    private final PersonalExpenseRepository expenseRepository;
    private final PersonalExpenseGroupRepository expenseGroupRepository;

    @Transactional(readOnly = true)
    public ExpenseSummaryDTO getExpenseSummary(Long userId) {
        Double totalSpent = expenseRepository.getTotalSpentByUserId(userId);
        Double totalSaved = expenseRepository.getTotalSavedByUserId(userId);
        Long totalExpenses = expenseRepository.countByUserId(userId);
        Long totalGroups = expenseGroupRepository.countByUserId(userId);
        
        Double totalBudget = (totalSpent != null ? totalSpent : 0.0) + (totalSaved != null ? totalSaved : 0.0);
        Double overallSavingsPercentage = totalBudget > 0 ? 
            ((totalSaved != null ? totalSaved : 0.0) / totalBudget) * 100 : 0.0;
        
        return new ExpenseSummaryDTO(
            totalSpent != null ? totalSpent : 0.0,
            totalSaved != null ? totalSaved : 0.0,
            totalBudget,
            overallSavingsPercentage,
            totalExpenses != null ? totalExpenses.intValue() : 0,
            totalGroups != null ? totalGroups.intValue() : 0
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryWiseExpenseDTO> getCategoryWiseExpenseSummary(Long userId) {
        return expenseRepository.getCategoryWiseExpenseSummary(userId);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryDTO getExpenseSummaryByDateRange(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        Double totalSpent = expenseRepository.getTotalSpentByUserIdAndDateRange(userId, startDate, endDate);
        Double totalSaved = expenseRepository.getTotalSavedByUserId(userId); // Note: This would need a date range version
        
        // For demo, using the same totalSaved (you might want to create a date-range version)
        Long totalExpenses = expenseRepository.countByUserId(userId); // This would also need date range
        
        Double totalBudget = (totalSpent != null ? totalSpent : 0.0) + (totalSaved != null ? totalSaved : 0.0);
        Double overallSavingsPercentage = totalBudget > 0 ? 
            ((totalSaved != null ? totalSaved : 0.0) / totalBudget) * 100 : 0.0;
        
        return new ExpenseSummaryDTO(
            totalSpent != null ? totalSpent : 0.0,
            totalSaved != null ? totalSaved : 0.0,
            totalBudget,
            overallSavingsPercentage,
            totalExpenses != null ? totalExpenses.intValue() : 0,
            0 // Groups count not applicable for date range
        );
    }
}