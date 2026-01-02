package com.expensetracker.app.service;

import com.expensetracker.app.dto.AddExpenseRequest;
import com.expensetracker.app.dto.ExpenseSplitDetail;
import com.expensetracker.app.entity.Expense;
import com.expensetracker.app.entity.ExpenseSplit;
import com.expensetracker.app.entity.Group;
import com.expensetracker.app.repository.ExpenseRepository;
import com.expensetracker.app.repository.GroupRepository;
import com.expensetracker.app.exception.ResourceNotFoundException;
import com.expensetracker.app.exception.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;

@Service
public class ExpenseService {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository, GroupRepository groupRepository) {
        this.expenseRepository = expenseRepository;
        this.groupRepository = groupRepository;
    }

    /**
     * Creates a new Expense and all associated ExpenseSplit records.
     */
    @Transactional
    public Expense createExpense(Long groupId, AddExpenseRequest request, String currentUsername)
            throws ResourceNotFoundException, ValidationException {
        
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
        
        System.out.println("=== DEBUG: CREATE EXPENSE MEMBERSHIP CHECK ===");
        System.out.println("DEBUG: Group ID: " + groupId);
        System.out.println("DEBUG: Current User: " + currentUsername);
        System.out.println("DEBUG: Request PaidBy: " + request.getPaidBy());
        System.out.println("DEBUG: Group Members Count: " + (group.getMembers() != null ? group.getMembers().size() : 0));
        
        if (group.getMembers() != null) {
            System.out.println("DEBUG: All Group Members:");
            group.getMembers().forEach(user -> {
                String username = user.getUsername();
                boolean isMatch = username.equalsIgnoreCase(currentUsername);
                System.out.println("  - " + username + (isMatch ? " *** MATCHES CURRENT USER ***" : ""));
            });
        }

        boolean isMemberResult = group.isMember(currentUsername);
        System.out.println("DEBUG: group.isMember('" + currentUsername + "') = " + isMemberResult);
        System.out.println("=============================================");

        if (!isMemberResult) {
            throw new AccessDeniedException("User '" + currentUsername + "' is not authorized to add expenses to this group. User is not a member.");
        }

        validateSplitTotals(request);

        Expense newExpense = new Expense();
        newExpense.setGroup(group);
        newExpense.setAmount(request.getAmount());
        newExpense.setDescription(request.getDescription());
        newExpense.setPaidByUsername(request.getPaidBy().toLowerCase());
        newExpense.setCreatedAt(OffsetDateTime.now());

        Expense savedExpense = expenseRepository.save(newExpense);

        List<ExpenseSplit> splits = request.getSplitDetails().stream()
                .map(detail -> createExpenseSplitFromDetail(savedExpense, detail))
                .collect(Collectors.toList());

        savedExpense.setSplits(splits);

        return savedExpense;
    }

    /**
     * Updates an existing expense.
     */
    @Transactional
    public Expense updateExpense(Long groupId, Long expenseId, AddExpenseRequest request, String currentUsername)
            throws ResourceNotFoundException, ValidationException {
        
        System.out.println("DEBUG: Updating expense - Group ID: " + groupId + ", Expense ID: " + expenseId);
        System.out.println("DEBUG: Current User: " + currentUsername);

        Expense existingExpense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));

        if (!existingExpense.getGroup().getId().equals(groupId)) {
            throw new ResourceNotFoundException("Expense", "id", expenseId);
        }

        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
                
        System.out.println("DEBUG: Group Members Count for Update: " + (group.getMembers() != null ? group.getMembers().size() : 0));
        
        if (!group.isMember(currentUsername)) {
            throw new AccessDeniedException("User is not authorized to update expenses in this group.");
        }

        if (!existingExpense.getPaidByUsername().equalsIgnoreCase(currentUsername)) {
            throw new AccessDeniedException("Only the user who paid for this expense can update it.");
        }

        validateSplitTotals(request);

        existingExpense.setAmount(request.getAmount());
        existingExpense.setDescription(request.getDescription());

        existingExpense.getSplits().clear();
        
        List<ExpenseSplit> newSplits = request.getSplitDetails().stream()
                .map(detail -> createExpenseSplitFromDetail(existingExpense, detail))
                .collect(Collectors.toList());
        
        existingExpense.getSplits().addAll(newSplits);

        return expenseRepository.save(existingExpense);
    }

    /**
     * Retrieves all expenses for a specific group, ordered by date.
     */
    public List<Expense> getExpensesByGroupId(Long groupId) {
        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    /**
     * Retrieves a single expense by ID with proper authorization checks.
     * CRITICAL FIX: Using findByIdAndGroupIdWithSplits instead of findById
     */
    @Transactional(readOnly = true)
    public Expense getExpenseById(Long groupId, Long expenseId, String currentUsername) {
        System.out.println("DEBUG: Service - Fetching expense - Group ID: " + groupId + ", Expense ID: " + expenseId);
        System.out.println("DEBUG: Service - Current User: " + currentUsername);

        // ✅ CRITICAL FIX: Use the method that checks group ID AND eagerly fetches splits
        Expense expense = expenseRepository.findByIdAndGroupIdWithSplits(expenseId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));

        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
                
        System.out.println("DEBUG: Service - Group Members Count: " + (group.getMembers() != null ? group.getMembers().size() : 0));

        if (!group.isMember(currentUsername)) {
            throw new AccessDeniedException("User is not authorized to view expenses in this group.");
        }

        System.out.println("DEBUG: Service - Successfully loaded expense with " + 
                          (expense.getSplits() != null ? expense.getSplits().size() : 0) + " splits");

        return expense;
    }

    // --- Private Helper Methods ---

    private void validateSplitTotals(AddExpenseRequest request) {
        BigDecimal totalOwed = request.getSplitDetails().stream()
                .map(ExpenseSplitDetail::getOwedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal difference = request.getAmount().subtract(totalOwed).abs();

        if (difference.compareTo(TOLERANCE) > 0) {
            throw new ValidationException(String.format(
                    "Expense split validation failed. Total amount ($%.2f) does not match total owed ($%.2f). Difference: $%.2f",
                    request.getAmount(), totalOwed, difference));
        }

        boolean payerPaidTotal = request.getSplitDetails().stream()
                .filter(d -> d.getMemberUsername().equalsIgnoreCase(request.getPaidBy()))
                .anyMatch(d -> d.getPaidAmount().compareTo(request.getAmount()) >= 0);

        if (!payerPaidTotal) {
            // Optional: Add more specific logic if needed
        }
    }

    private ExpenseSplit createExpenseSplitFromDetail(Expense expense, ExpenseSplitDetail detail) {
        BigDecimal netBalance = detail.getPaidAmount().subtract(detail.getOwedAmount());

        ExpenseSplit split = new ExpenseSplit();
        split.setExpense(expense);
        split.setMemberUsername(detail.getMemberUsername().toLowerCase());
        split.setOwedAmount(detail.getOwedAmount().setScale(2, RoundingMode.HALF_UP));
        split.setPaidAmount(detail.getPaidAmount().setScale(2, RoundingMode.HALF_UP));
        split.setNetBalance(netBalance.setScale(2, RoundingMode.HALF_UP));

        return split;
    }
}