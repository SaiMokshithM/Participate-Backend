package com.participatehere.service;

import com.participatehere.dto.response.ParticipationResponse;
import com.participatehere.entity.*;
import com.participatehere.exception.BadRequestException;
import com.participatehere.exception.ResourceNotFoundException;
import com.participatehere.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public ParticipationResponse enroll(Long activityId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + activityId));

        if (participationRepository.existsByUserIdAndActivityId(user.getId(), activityId)) {
            throw new BadRequestException("You are already enrolled in this activity.");
        }
        if (activity.getEnrolled() >= activity.getCapacity()) {
            throw new BadRequestException("This activity is full.");
        }

        Participation participation = Participation.builder()
                .user(user).activity(activity).role("Member").status(Participation.Status.ACTIVE).score(0)
                .build();
        participationRepository.save(participation);

        activity.setEnrolled(activity.getEnrolled() + 1);
        activityRepository.save(activity);

        // Auto notification
        Notification notif = Notification.builder()
                .user(user)
                .title("Enrolled: " + activity.getName())
                .message("You have successfully enrolled in " + activity.getName() + ". Schedule: " + activity.getSchedule())
                .type("SUCCESS")
                .isRead(false)
                .build();
        notificationRepository.save(notif);

        return toResponse(participation);
    }

    @Transactional
    public void unenroll(Long activityId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        Participation participation = participationRepository
                .findByUserIdAndActivityId(user.getId(), activityId)
                .orElseThrow(() -> new BadRequestException("You are not enrolled in this activity."));

        participationRepository.delete(participation);

        if (activity.getEnrolled() > 0) {
            activity.setEnrolled(activity.getEnrolled() - 1);
            activityRepository.save(activity);
        }
    }

    public List<ParticipationResponse> getMyParticipations(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return participationRepository.findByUserIdWithActivity(user.getId())
                .stream().map(this::toResponse).toList();
    }

    public List<ParticipationResponse> getParticipantsByActivity(Long activityId) {
        return participationRepository.findByActivityIdWithUser(activityId)
                .stream().map(this::toResponse).toList();
    }

    public Map<String, Object> getMyStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Participation> list = participationRepository.findByUserIdWithActivity(user.getId());
        long total = list.size();
        long active = list.stream().filter(p -> p.getStatus() == Participation.Status.ACTIVE).count();
        double avgScore = list.stream().mapToInt(p -> p.getScore() == null ? 0 : p.getScore()).average().orElse(0.0);
        return Map.of("totalActivities", total, "activeActivities", active, "averageScore", avgScore);
    }

    private ParticipationResponse toResponse(Participation p) {
        return ParticipationResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .userName(p.getUser().getFirstName() + " " + p.getUser().getLastName())
                .userEmail(p.getUser().getEmail())
                .activityId(p.getActivity().getId())
                .activityName(p.getActivity().getName())
                .activityCategory(p.getActivity().getCategory())
                .activityImageUrl(p.getActivity().getImageUrl())
                .role(p.getRole())
                .status(p.getStatus().name())
                .score(p.getScore())
                .joinedAt(p.getJoinedAt())
                .build();
    }
}
