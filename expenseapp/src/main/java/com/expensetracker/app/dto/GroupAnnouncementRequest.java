package com.expensetracker.app.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new Group Announcement.
 * The 'group' and 'createdBy' fields will be set by the service layer 
 * using the URL path and the JWT token, not directly from this DTO.
 */
@Data
public class GroupAnnouncementRequest {

    @Size(max = 255, message = "Title must be no more than 255 characters.")
    private String title;

    @NotBlank(message = "Description is required.")
    @Size(min = 5, message = "Description must be at least 5 characters long.")
    private String description;
    
    // Note: No fields for groupId or createdByUserId are included here, 
    // as they will be extracted from the API path and the user's JWT token 
    // in the controller/service to prevent tampering.
}