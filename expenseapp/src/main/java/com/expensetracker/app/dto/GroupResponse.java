package com.expensetracker.app.dto;

import lombok.Data;
import lombok.Builder;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class GroupResponse {
    private Long id;
    private String name;
    private String description;
    private String createdBy;
    private OffsetDateTime createdAt;
    private List<String> members; // List of member usernames
    private Integer totalMembers;
}