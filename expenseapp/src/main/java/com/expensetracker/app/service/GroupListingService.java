package com.expensetracker.app.service;

import com.expensetracker.app.dto.GroupResponse;
import com.expensetracker.app.entity.Group;
import com.expensetracker.app.entity.User;
import com.expensetracker.app.repository.GroupListingRepository;
import com.expensetracker.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupListingService {

    @Autowired
    private GroupListingRepository groupListingRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(String username) {
        
        // 1. Find the User entity for the authenticated user
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // 2. Fetch the groups where the user is a member
        List<Group> groups = groupListingRepository.findAllByMembersContaining(user);
        
        // 3. Map entities to DTOs
        return groups.stream()
            .map(this::mapToGroupResponse)
            .collect(Collectors.toList());
    }

    private GroupResponse mapToGroupResponse(Group group) {
        // Collect usernames for the DTO
        List<String> memberUsernames = group.getMembers().stream()
            .map(User::getUsername)
            .sorted()
            .collect(Collectors.toList());
            
        return GroupResponse.builder()
            .id(group.getId())
            .name(group.getName())
            .description(group.getDescription())
            .createdBy(group.getCreatedBy() != null ? group.getCreatedBy().getUsername() : "N/A")
            .createdAt(group.getCreatedAt())
            .members(memberUsernames)
            .totalMembers(memberUsernames.size())
            .build();
    }
}