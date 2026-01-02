package com.expensetracker.app.service;

import com.expensetracker.app.dto.GroupAnnouncementRequest;
import com.expensetracker.app.dto.GroupAnnouncementResponse;
import com.expensetracker.app.entity.Group;
import com.expensetracker.app.entity.GroupAnnouncement;
import com.expensetracker.app.entity.User;
import com.expensetracker.app.repository.GroupAnnouncementRepository;
import com.expensetracker.app.repository.GroupRepository;
import com.expensetracker.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final GroupAnnouncementRepository announcementRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new announcement for a group after verifying the user is a member.
     *
     * @param groupId The ID of the group the announcement belongs to.
     * @param requestingUsername The username of the user making the request (from JWT).
     * @param request The DTO containing the announcement title and description.
     * @return The response DTO for the newly created announcement.
     */
    @Transactional
    public GroupAnnouncementResponse createAnnouncement(
            Long groupId,
            String requestingUsername,
            GroupAnnouncementRequest request) {

        // 1. Fetch Group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with ID: " + groupId));

        // 2. Fetch User (Creator)
        User creator = userRepository.findByUsername(requestingUsername.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found: " + requestingUsername));

        // 3. Authorization Check: Only existing members can create announcements.
        boolean isMember = group.getMembers().stream()
                .anyMatch(member -> member.getUsername().equalsIgnoreCase(requestingUsername));

        if (!isMember) {
            throw new AccessDeniedException("Access Denied: Only group members can post announcements.");
        }

        // 4. Create Entity from Request
        GroupAnnouncement announcement = GroupAnnouncement.builder()
                .group(group)
                .createdBy(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        // 5. Save and return mapped DTO
        announcement = announcementRepository.save(announcement);
        
        return mapToResponse(announcement);
    }

    /**
     * Fetches all announcements for a specific group, ordered by creation date.
     *
     * @param groupId The ID of the group.
     * @return A list of GroupAnnouncementResponse DTOs.
     */
    public List<GroupAnnouncementResponse> getAnnouncementsByGroup(Long groupId) {
        
        // We can skip the user check here, assuming the Controller has already 
        // authorized the user to view this group's details.
        
        List<GroupAnnouncement> announcements = announcementRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        
        return announcements.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps the GroupAnnouncement entity to the GroupAnnouncementResponse DTO.
     */
    private GroupAnnouncementResponse mapToResponse(GroupAnnouncement announcement) {
        return GroupAnnouncementResponse.builder()
                .id(announcement.getId())
                .groupId(announcement.getGroup().getId())
                .createdByUsername(announcement.getCreatedBy().getUsername())
                .title(announcement.getTitle())
                .description(announcement.getDescription())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}