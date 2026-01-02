package com.expensetracker.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonalExpenseGroupRequestDTO {
    private String title;
    private String description;
    private OffsetDateTime fromDate;
    private OffsetDateTime toDate;
}