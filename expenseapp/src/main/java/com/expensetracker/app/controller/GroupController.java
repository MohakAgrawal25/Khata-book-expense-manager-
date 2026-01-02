package com.expensetracker.app.controller;

import com.expensetracker.app.dto.*;
import com.expensetracker.app.service.GroupService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    // Create a new group
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            GroupResponse response = groupService.createGroup(request, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get all groups for logged-in user
    @GetMapping
    public ResponseEntity<?> getUserGroups(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<GroupResponse> groups = groupService.getUserGroups(username);
            return ResponseEntity.ok(groups);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get specific group details
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupById(
            @PathVariable Long groupId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            GroupResponse response = groupService.getGroupById(groupId, username);
            if (response == null) {
                // If the object is null, return 404 Not Found.
                // The frontend will now correctly show the 'Failed to Load Group Data' box.
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found or access denied.");
            }

            // 2. If not null, return 200 OK with the actual JSON body
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Add a member to existing group (optional feature)
    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMemberToGroup(
            @PathVariable Long groupId,
            @RequestParam String username,
            Authentication authentication) {
        try {
            String requestingUsername = authentication.getName();
            GroupResponse response = groupService.addMemberToGroup(groupId, username, requestingUsername);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}