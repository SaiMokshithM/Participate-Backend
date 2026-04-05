package com.participatehere.controller;

import com.participatehere.dto.response.ParticipationResponse;
import com.participatehere.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/participation")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    @PostMapping("/enroll/{activityId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ParticipationResponse> enroll(
            @PathVariable Long activityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(participationService.enroll(activityId, userDetails.getUsername()));
    }

    @DeleteMapping("/unenroll/{activityId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> unenroll(
            @PathVariable Long activityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        participationService.unenroll(activityId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<ParticipationResponse>> getMyParticipations(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(participationService.getMyParticipations(userDetails.getUsername()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMyStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(participationService.getMyStats(userDetails.getUsername()));
    }

    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ParticipationResponse>> getParticipantsByActivity(@PathVariable Long activityId) {
        return ResponseEntity.ok(participationService.getParticipantsByActivity(activityId));
    }
}
