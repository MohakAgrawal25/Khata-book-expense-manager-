package com.expensetracker.app.service;

import com.expensetracker.app.dto.CategoryWiseExpenseDTO;
import com.expensetracker.app.dto.PersonalExpenseDTO;
import com.expensetracker.app.entity.ExpenseCategory;
import com.expensetracker.app.entity.PersonalExpense;
import com.expensetracker.app.entity.PersonalExpenseGroup;
import com.expensetracker.app.entity.User;
import com.expensetracker.app.repository.PersonalExpenseGroupRepository;
import com.expensetracker.app.repository.PersonalExpenseRepository;
import com.expensetracker.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalExpenseService {

    private final PersonalExpenseRepository expenseRepository;
    private final PersonalExpenseGroupRepository expenseGroupRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PersonalExpenseDTO> getAllExpensesByUserId(Long userId) {
        log.info("Getting all expenses for user ID: {}", userId);
        List<PersonalExpense> expenses = expenseRepository.findByUserId(userId);
        log.info("Found {} expenses for user ID: {}", expenses.size(), userId);
        return expenses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PersonalExpenseDTO getExpenseById(Long expenseId, Long userId) {
        log.info("Getting expense by ID: {} for user ID: {}", expenseId, userId);
        PersonalExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + expenseId));
        
        if (!expense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this expense");
        }
        
        return convertToDTO(expense);
    }

    @Transactional
    public PersonalExpenseDTO createExpense(PersonalExpense expense, Long userId) {
        log.info("Creating expense for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        expense.setUser(user);
        
        // Set expense group if provided
        if (expense.getExpenseGroup() != null && expense.getExpenseGroup().getId() != null) {
            PersonalExpenseGroup group = expenseGroupRepository.findById(expense.getExpenseGroup().getId())
                    .orElseThrow(() -> new RuntimeException("Expense group not found with id: " + expense.getExpenseGroup().getId()));
            
            if (!group.getUser().getId().equals(userId)) {
                throw new RuntimeException("Access denied for this expense group");
            }
            expense.setExpenseGroup(group);
        }
        
        // Validate amounts
        if (expense.getTotalSpent() < 0 || expense.getAmountSaved() < 0) {
            throw new RuntimeException("Amounts cannot be negative");
        }
        
        // Set timestamps if not set
        if (expense.getCreatedAt() == null) {
            expense.setCreatedAt(OffsetDateTime.now());
        }
        expense.setUpdatedAt(OffsetDateTime.now());
        
        PersonalExpense savedExpense = expenseRepository.save(expense);
        log.info("Created expense with id: {} for user: {}", savedExpense.getId(), userId);
        
        return convertToDTO(savedExpense);
    }

    @Transactional
    public PersonalExpenseDTO updateExpense(Long expenseId, PersonalExpense updatedExpense, Long userId) {
        log.info("Updating expense ID: {} for user ID: {}", expenseId, userId);
        
        PersonalExpense existingExpense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + expenseId));
        
        if (!existingExpense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this expense");
        }
        
        // Validate amounts
        if (updatedExpense.getTotalSpent() < 0 || updatedExpense.getAmountSaved() < 0) {
            throw new RuntimeException("Amounts cannot be negative");
        }
        
        // Update expense group if provided
        if (updatedExpense.getExpenseGroup() != null && updatedExpense.getExpenseGroup().getId() != null) {
            PersonalExpenseGroup group = expenseGroupRepository.findById(updatedExpense.getExpenseGroup().getId())
                    .orElseThrow(() -> new RuntimeException("Expense group not found with id: " + updatedExpense.getExpenseGroup().getId()));
            
            if (!group.getUser().getId().equals(userId)) {
                throw new RuntimeException("Access denied for this expense group");
            }
            existingExpense.setExpenseGroup(group);
        } else {
            existingExpense.setExpenseGroup(null);
        }
        
        // Update fields
        existingExpense.setTitle(updatedExpense.getTitle());
        existingExpense.setDescription(updatedExpense.getDescription());
        existingExpense.setCategory(updatedExpense.getCategory());
        existingExpense.setTotalSpent(updatedExpense.getTotalSpent());
        existingExpense.setAmountSaved(updatedExpense.getAmountSaved());
        existingExpense.setExpenseDate(updatedExpense.getExpenseDate());
        existingExpense.setPaymentMethod(updatedExpense.getPaymentMethod());
        existingExpense.setUpdatedAt(OffsetDateTime.now());
        
        PersonalExpense savedExpense = expenseRepository.save(existingExpense);
        log.info("Updated expense with id: {}", expenseId);
        
        return convertToDTO(savedExpense);
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        log.info("Deleting expense ID: {} for user ID: {}", expenseId, userId);
        
        PersonalExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + expenseId));
        
        if (!expense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this expense");
        }
        
        expenseRepository.delete(expense);
        log.info("Deleted expense with id: {}", expenseId);
    }

    @Transactional(readOnly = true)
    public List<PersonalExpenseDTO> getExpensesByCategory(Long userId, ExpenseCategory category) {
        log.info("Getting expenses by category: {} for user ID: {}", category, userId);
        List<PersonalExpense> expenses = expenseRepository.findByUserIdAndCategory(userId, category);
        return expenses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonalExpenseDTO> getExpensesByDateRange(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        log.info("Getting expenses by date range: {} to {} for user ID: {}", startDate, endDate, userId);
        List<PersonalExpense> expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, startDate, endDate);
        return expenses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonalExpenseDTO> getExpensesByGroupId(Long groupId, Long userId) {
        log.info("Getting expenses by group ID: {} for user ID: {}", groupId, userId);
        
        PersonalExpenseGroup group = expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Expense group not found with id: " + groupId));
        
        if (!group.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this expense group");
        }
        
        List<PersonalExpense> expenses = expenseRepository.findByExpenseGroupId(groupId);
        log.info("Found {} expenses for group ID: {}", expenses.size(), groupId);
        return expenses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonalExpenseDTO> searchExpenses(Long userId, String keyword) {
        log.info("Searching expenses with keyword: '{}' for user ID: {}", keyword, userId);
        
        List<PersonalExpense> expensesByTitle = expenseRepository.findByUserIdAndTitleContainingIgnoreCase(userId, keyword);
        List<PersonalExpense> expensesByDescription = expenseRepository.findByUserIdAndDescriptionContainingIgnoreCase(userId, keyword);
        
        // Combine and remove duplicates using a Set
        List<PersonalExpense> combinedExpenses = new ArrayList<>(expensesByTitle);
        for (PersonalExpense expense : expensesByDescription) {
            if (!combinedExpenses.contains(expense)) {
                combinedExpenses.add(expense);
            }
        }
        
        log.info("Found {} expenses for search keyword: '{}'", combinedExpenses.size(), keyword);
        return combinedExpenses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonalExpenseDTO> getRecentExpenses(Long userId, int limit) {
        log.info("Getting recent {} expenses for user ID: {}", limit, userId);
        
        // FIXED: Use the native query method instead of the problematic one
        List<PersonalExpense> expenses = expenseRepository.findRecentExpensesByUserIdWithLimit(userId, limit);
        
        log.info("Found {} recent expenses for user ID: {}", expenses.size(), userId);
        return expenses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryWiseExpenseDTO> getCategoryWiseSummary(Long userId) {
        log.info("Getting category-wise summary for user ID: {}", userId);
        return expenseRepository.getCategoryWiseExpenseSummary(userId);
    }

    @Transactional(readOnly = true)
    public Double getTotalSpentByUserId(Long userId) {
        log.info("Getting total spent for user ID: {}", userId);
        Double totalSpent = expenseRepository.getTotalSpentByUserId(userId);
        return totalSpent != null ? totalSpent : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getTotalSavedByUserId(Long userId) {
        log.info("Getting total saved for user ID: {}", userId);
        Double totalSaved = expenseRepository.getTotalSavedByUserId(userId);
        return totalSaved != null ? totalSaved : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getSavingsPercentageByUserId(Long userId) {
        log.info("Getting savings percentage for user ID: {}", userId);
        Double totalSpent = getTotalSpentByUserId(userId);
        Double totalSaved = getTotalSavedByUserId(userId);
        
        if (totalSpent == 0.0) {
            return 0.0;
        }
        
        return (totalSaved / totalSpent) * 100;
    }

    private PersonalExpenseDTO convertToDTO(PersonalExpense expense) {
        try {
            // Calculate savings percentage for this expense
            Double savingsPercentage = null;
            if (expense.getTotalSpent() != null && expense.getTotalSpent() > 0 && expense.getAmountSaved() != null) {
                savingsPercentage = (expense.getAmountSaved() / expense.getTotalSpent()) * 100;
            }
            
            // Check if expense has savings
            Boolean hasSavings = expense.getAmountSaved() != null && expense.getAmountSaved() > 0;
            
            return PersonalExpenseDTO.builder()
                    .id(expense.getId())
                    .title(expense.getTitle())
                    .description(expense.getDescription())
                    .category(expense.getCategory())
                    .totalSpent(expense.getTotalSpent())
                    .amountSaved(expense.getAmountSaved())
                    .expenseDate(expense.getExpenseDate())
                    .paymentMethod(expense.getPaymentMethod())
                    .createdAt(expense.getCreatedAt())
                    .updatedAt(expense.getUpdatedAt())
                    .expenseGroupId(expense.getExpenseGroup() != null ? expense.getExpenseGroup().getId() : null)
                    .expenseGroupTitle(expense.getExpenseGroup() != null ? expense.getExpenseGroup().getTitle() : null)
                    .userId(expense.getUser().getId())
                    .username(expense.getUser().getUsername())
                    .totalBudget(expense.getTotalBudget())
                    .savingsPercentage(savingsPercentage)
                    .hasSavings(hasSavings)
                    .build();
        } catch (Exception e) {
            log.error("Error converting expense to DTO for expense ID: {}", expense.getId(), e);
            throw new RuntimeException("Error converting expense to DTO: " + e.getMessage());
        }
    }
}