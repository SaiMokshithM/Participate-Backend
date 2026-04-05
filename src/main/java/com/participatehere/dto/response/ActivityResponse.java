package com.participatehere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ActivityResponse {
    private Long id;
    private String name;
    private String category;
    private String description;
    private Integer capacity;
    private Integer enrolled;
    private String organizer;
    private String schedule;
    private String location;
    private String status;
    private String imageUrl;
    private LocalDateTime createdAt;
    private boolean enrolledByCurrentUser;
}
