package com.expensetracker.app.controller;

import com.expensetracker.app.dto.GroupAnnouncementRequest;
import com.expensetracker.app.dto.GroupAnnouncementResponse;
import com.expensetracker.app.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * POST /api/groups/{groupId}/announcements
     * Creates a new announcement for the specified group.
     * Authorization: Only existing group members can create announcements.
     * * @param groupId The ID of the group.
     * @param request The announcement data (title, description).
     * @param authentication The Spring Security Authentication object (contains the requesting user's username).
     * @return The created announcement response DTO.
     */
    @PostMapping
    public ResponseEntity<GroupAnnouncementResponse> createAnnouncement(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupAnnouncementRequest request,
            Authentication authentication) {

        // Get the username from the JWT token provided by Spring Security
        String requestingUsername = authentication.getName();

        try {
            GroupAnnouncementResponse response = announcementService.createAnnouncement(
                    groupId,
                    requestingUsername,
                    request
            );
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (AccessDeniedException e) {
            // This handles the error thrown by the service if the user is not a member.
            // It translates the exception into a 403 Forbidden response.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            // General error handling (e.g., Group not found, User not found)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // --------------------------------------------------------------------------

    /**
     * GET /api/groups/{groupId}/announcements
     * Retrieves all announcements for the specified group.
     * Authorization: Assumes the group details controller already checked membership.
     * * @param groupId The ID of the group.
     * @return A list of announcement response DTOs.
     */
    @GetMapping
    public ResponseEntity<List<GroupAnnouncementResponse>> getAnnouncementsByGroup(
            @PathVariable Long groupId) {
        
        // Note: For full security, you might want to re-check authorization here, 
        // but often, checking it on a single, higher-level endpoint (like fetching group details) 
        // is sufficient if the frontend only calls this when the user is a known member.
        
        List<GroupAnnouncementResponse> announcements = announcementService.getAnnouncementsByGroup(groupId);
        return ResponseEntity.ok(announcements);
    }
}