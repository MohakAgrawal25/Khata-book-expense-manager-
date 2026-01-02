package com.expensetracker.app.repository;

import com.expensetracker.app.dto.CategoryWiseExpenseDTO;
import com.expensetracker.app.entity.PersonalExpense;

import java.time.OffsetDateTime;
import java.util.List;

public interface PersonalExpenseRepositoryCustom {
    
    List<CategoryWiseExpenseDTO> getCategoryWiseExpenseSummary(Long userId);
    
    List<CategoryWiseExpenseDTO> getCategoryWiseExpenseSummaryByDateRange(
            Long userId, OffsetDateTime startDate, OffsetDateTime endDate);
    
    List<PersonalExpense> findExpensesWithHighSavings(Long userId, Double savingsThreshold);
}