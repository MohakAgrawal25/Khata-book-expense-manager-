package com.expensetracker.app.controller;

import com.expensetracker.app.dto.GroupResponse;
import com.expensetracker.app.service.GroupListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my-groups") // Unique endpoint for listing
public class GroupListingController {

    @Autowired
    private GroupListingService groupListingService;

    /**
     * Endpoint: GET /api/my-groups
     * Retrieves all groups the currently authenticated user belongs to.
     */
    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroupsForCurrentUser(Authentication authentication) {
        // Get the username from the Spring Security context
        String username = authentication.getName();
        
        List<GroupResponse> groups = groupListingService.getUserGroups(username);
        
        return ResponseEntity.ok(groups);
    }
}