package com.expensetracker.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CreateGroupRequest {
    
    // Group name must be present and not just whitespace
    @NotBlank(message = "Group name is required.")
    @Size(min = 3, max = 100, message = "Group name must be between 3 and 100 characters.")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters.")
    private String description;
    
    // Explicitly set the maximum size to 200 members.
    // The list must not be null and must contain at least one username (the creator).
    @NotEmpty(message = "The group must contain at least one member username (the creator).")
    @Size(max = 200, message = "A group cannot exceed 200 members.") // <-- CRUCIAL MAX SIZE FIX
    private List<String> memberUsernames; 
}
