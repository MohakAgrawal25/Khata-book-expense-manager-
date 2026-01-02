package com.expensetracker.app.service;

import com.expensetracker.app.dto.*;
import com.expensetracker.app.entity.*;
import com.expensetracker.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(timeout = 600)
    public GroupResponse createGroup(CreateGroupRequest request, String creatorUsername) {

        String normalizedCreatorUsername = creatorUsername.trim().toLowerCase();

        // 1. Find the creator from the security context
        User creator = userRepository.findByUsername(normalizedCreatorUsername)
                .orElseThrow(() -> new RuntimeException("Creator not found for username: " + creatorUsername));

        List<String> rawMemberUsernames = request.getMemberUsernames();
        if (rawMemberUsernames == null || rawMemberUsernames.isEmpty()) {
            throw new RuntimeException("Please provide member usernames");
        }

        // 2. Normalize ALL usernames and collect unique ones for batch lookup
        Set<String> normalizedUsernames = rawMemberUsernames.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(u -> !u.isBlank())
                .collect(Collectors.toSet());

        // **CRUCIAL FIX:** BATCH LOOKUP: Find all User objects in a SINGLE query.
        List<User> foundMembers = userRepository.findAllByUsernameIn(normalizedUsernames);

        // 3. Validate that all requested users were found
        if (foundMembers.size() != normalizedUsernames.size()) {

            // Determine which user was not found for a precise error message
            Set<String> foundUsernames = foundMembers.stream()
                    .map(User::getUsername)
                    .collect(Collectors.toSet());

            String missingUsername = normalizedUsernames.stream()
                    .filter(u -> !foundUsernames.contains(u))
                    .findFirst()
                    .orElse("Unknown user or potential duplicate error");

            // This error is sent back to the frontend
            throw new RuntimeException(
                    "User not found: " + missingUsername + ". Please check the username is valid and exists.");
        }

        // 4. Build the final member set and ensure creator is included
        // (The creator should already be included in the request body from the
        // frontend)
        Set<User> members = new HashSet<>(foundMembers);
        members.add(creator); // Guarantees creator is in the final set

        if (members.isEmpty()) {
            throw new RuntimeException("Group creation failed: No valid members found.");
        }

        // 5. Create and save the group
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .members(members)
                .build();

        group = groupRepository.save(group);

        return buildGroupResponse(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(String username) {
        /* Implementation omitted for brevity */
        return Collections.emptyList();
    }

    // =========================================================================
    // CORRECTED IMPLEMENTATION FOR getGroupById
    // =========================================================================

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long groupId, String username) {

        // 1. Fetch the Group Entity from the database
        Optional<Group> groupOptional = groupRepository.findById(groupId);

        if (groupOptional.isEmpty()) {
            // Group not found. Returning null causes the Controller to return 404.
            return null;
        }

        Group group = groupOptional.get();

        // 2. Access Control: Check if the requesting user is a member of the group.
        // This is crucial for security.
        boolean isMember = group.getMembers().stream()
                .anyMatch(user -> user.getUsername().equalsIgnoreCase(username));

        if (!isMember) {
            // User is not authorized. Returning null causes the Controller to return 404.
            System.err.println("Access Denied: User " + username + " is not a member of Group ID " + groupId);
            return null;
        }

        // 3. If found and authorized, map the entity to the GroupResponse DTO
        return buildGroupResponse(group);
    }

    // =========================================================================

    @Transactional
    public GroupResponse addMemberToGroup(Long groupId, String memberUsername, String requestingUsername) {

        // 1. Fetch Group and perform initial check
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group with ID " + groupId + " not found."));

        // 2. Authorization Check: Check if the requesting user is an EXISTING member
        boolean isMember = group.getMembers().stream()
                .anyMatch(member -> member.getUsername().equalsIgnoreCase(requestingUsername));

        if (!isMember) {
            // If the requesting user is NOT a member, deny access.
            throw new AccessDeniedException("Access Denied: Only existing group members can add new members.");
        }
        // If the user is a member, authorization is granted.

        // 3. Find the User to be added
        User memberToAdd = userRepository.findByUsername(memberUsername.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("User '" + memberUsername + "' not found in the system."));

        // 4. Check if the user is ALREADY a member
        boolean alreadyMember = group.getMembers().stream()
                .anyMatch(member -> member.getUsername().equalsIgnoreCase(memberUsername));

        if (alreadyMember) {
            throw new RuntimeException("User '" + memberUsername + "' is already a member of this group.");
        }

        // 5. Add the new member to the group's member set
        group.getMembers().add(memberToAdd);
        memberToAdd.getGroups().add(group);

        // Save the updated group (transaction handles this, but explicit save is safe)
        group = groupRepository.save(group);

        // 6. Return the updated group response
        return buildGroupResponse(group);
    }

    private GroupResponse buildGroupResponse(Group group) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy().getUsername())
                .createdAt(group.getCreatedAt())
                .members(group.getMembers().stream()
                        .map(User::getUsername)
                        .sorted()
                        .collect(Collectors.toList()))
                .totalMembers(group.getMembers().size())
                .build();
    }
}