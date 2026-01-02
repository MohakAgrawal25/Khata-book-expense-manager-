package com.expensetracker.app.dto;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

/**
 * DTO for sending Group Announcement data back to the client.
 */
@Data
@Builder
public class GroupAnnouncementResponse {

    private Long id;
    
    // We send back the ID of the group it belongs to
    private Long groupId;
    
    // Send back the username of the creator, not the full User object
    private String createdByUsername;
    
    private String title;
    private String description;
    
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}