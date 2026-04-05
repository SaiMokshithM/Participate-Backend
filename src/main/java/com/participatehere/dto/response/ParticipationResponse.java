package com.participatehere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ParticipationResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long activityId;
    private String activityName;
    private String activityCategory;
    private String activityImageUrl;
    private String role;
    private String status;
    private Integer score;
    private LocalDateTime joinedAt;
}
