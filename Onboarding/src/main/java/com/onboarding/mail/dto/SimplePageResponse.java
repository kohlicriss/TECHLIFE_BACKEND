package com.onboarding.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SimplePageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private boolean empty;
}
