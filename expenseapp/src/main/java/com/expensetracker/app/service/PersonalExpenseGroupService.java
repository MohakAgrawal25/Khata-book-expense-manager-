package com.expensetracker.app.service;

import com.expensetracker.app.dto.PersonalExpenseGroupDTO;
import com.expensetracker.app.entity.PersonalExpenseGroup;
import com.expensetracker.app.entity.User;
import com.expensetracker.app.repository.PersonalExpenseGroupRepository;
import com.expensetracker.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalExpenseGroupService {

    private final PersonalExpenseGroupRepository expenseGroupRepository;
    private final UserRepository userRepository;

    // Existing methods...

    // Username-based methods for controller
    @Transactional(readOnly = true)
    public List<PersonalExpenseGroupDTO> getUserGroups(String username) {
        User user = getUserByUsername(username);
        return getAllGroupsByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public PersonalExpenseGroupDTO getGroupById(Long groupId, String username) {
        User user = getUserByUsername(username);
        return getGroupById(groupId, user.getId());
    }

    @Transactional
    public PersonalExpenseGroupDTO createGroup(PersonalExpenseGroup group, String username) {
        User user = getUserByUsername(username);
        return createGroup(group, user.getId());
    }

    @Transactional
    public PersonalExpenseGroupDTO updateGroup(Long groupId, PersonalExpenseGroup updatedGroup, String username) {
        User user = getUserByUsername(username);
        return updateGroup(groupId, updatedGroup, user.getId());
    }

    @Transactional
    public void deleteGroup(Long groupId, String username) {
        User user = getUserByUsername(username);
        deleteGroup(groupId, user.getId());
    }

    @Transactional(readOnly = true)
    public List<PersonalExpenseGroupDTO> searchGroupsByTitle(String username, String title) {
        User user = getUserByUsername(username);
        return searchGroupsByTitle(user.getId(), title);
    }

    @Transactional(readOnly = true)
    public List<PersonalExpenseGroupDTO> getGroupsByDateRange(String username, OffsetDateTime startDate, OffsetDateTime endDate) {
        User user = getUserByUsername(username);
        return getGroupsByDateRange(user.getId(), startDate, endDate);
    }

    // Helper method to get user by username
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    // Your existing methods remain the same...
    @Transactional(readOnly = true)
    public List<PersonalExpenseGroupDTO> getAllGroupsByUserId(Long userId) {
        List<PersonalExpenseGroup> groups = expenseGroupRepository.findByUserIdWithExpenses(userId);
        return groups.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PersonalExpenseGroupDTO getGroupById(Long groupId, Long userId) {
        PersonalExpenseGroup group = expenseGroupRepository.findByIdWithExpenses(groupId)
                .orElseThrow(() -> new RuntimeException("Expense group not found with id: " + groupId));
        
        if (!group.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this expense group");
        }
        
        return convertToDTO(group);
    }

    @Transactional
    public PersonalExpenseGroupDTO createGroup(PersonalExpenseGroup group, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        group.setUser(user);
        
        if (expenseGroupRepository.existsByUserIdAndTitle(userId, group.getTitle())) {
            throw new RuntimeException("Expense group with this title already exists");
        }
        
        PersonalExpenseGroup savedGroup = expenseGroupRepository.save(group);
        log.info("Created expense group with id: {} for user: {}", savedGroup.getId(), userId);
        
        return convertToDTO(savedGroup);
    }

    @Transactional
    public PersonalExpenseGroupDTO updateGroup(Long groupId, PersonalExpenseGroup updatedGroup, Long userId) {
        PersonalExpenseGroup existingGroup = expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Expense group not found with id: " + groupId));
        
        if (!existingGroup.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this expense group");
        }
        
        if (!existingGroup.getTitle().equals(updatedGroup.getTitle()) &&
            expenseGroupRepository.existsByUserIdAndTitle(userId, updatedGroup.getTitle())) {
            throw new RuntimeException("Expense group with this title already exists");
        }
        
        existingGroup.setTitle(updatedGroup.getTitle());
        existingGroup.setDescription(updatedGroup.getDescription());
        existingGroup.setFromDate(updatedGroup.getFromDate());
        existingGroup.setToDate(updatedGroup.getToDate());
        
        PersonalExpenseGroup savedGroup = expenseGroupRepository.save(existingGroup);
        log.info("Updated expense group with id: {}", groupId);
        
        return convertToDTO(savedGroup);
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        PersonalExpenseGroup group = expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Expense group not found with id: " + groupId));
        
        if (!group.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied for this expense group");
        }
        
        expenseGroupRepository.delete(group);
        log.info("Deleted expense group with id: {}", groupId);
    }

    @Transactional(readOnly = true)
    public List<PersonalExpenseGroupDTO> searchGroupsByTitle(Long userId, String title) {
        List<PersonalExpenseGroup> groups = expenseGroupRepository.findByUserIdAndTitleContainingIgnoreCase(userId, title);
        return groups.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersonalExpenseGroupDTO> getGroupsByDateRange(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        List<PersonalExpenseGroup> groups = expenseGroupRepository.findByUserIdAndFromDateAfterAndToDateBefore(userId, startDate, endDate);
        return groups.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private PersonalExpenseGroupDTO convertToDTO(PersonalExpenseGroup group) {
        return PersonalExpenseGroupDTO.builder()
                .id(group.getId())
                .title(group.getTitle())
                .description(group.getDescription())
                .fromDate(group.getFromDate())
                .toDate(group.getToDate())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .userId(group.getUser().getId())
                .username(group.getUser().getUsername())
                .expenseCount(group.getExpenseCount())
                .totalSpentInGroup(group.getTotalSpentInGroup())
                .totalSavedInGroup(group.getTotalSavedInGroup())
                .totalBudgetForGroup(group.getTotalBudgetForGroup())
                .groupSavingsPercentage(group.getGroupSavingsPercentage())
                .build();
    }
}