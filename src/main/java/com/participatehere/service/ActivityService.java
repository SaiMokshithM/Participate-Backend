package com.participatehere.service;

import com.participatehere.dto.request.ActivityRequest;
import com.participatehere.dto.response.ActivityResponse;
import com.participatehere.entity.Activity;
import com.participatehere.entity.User;
import com.participatehere.exception.BadRequestException;
import com.participatehere.exception.ResourceNotFoundException;
import com.participatehere.repository.ActivityRepository;
import com.participatehere.repository.ParticipationRepository;
import com.participatehere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;

    public Page<ActivityResponse> getAllActivities(int page, int size, String sort, String category, String search,
            String currentUserEmail) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sort.equals("oldest") ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt"));

        Long currentUserId = getCurrentUserId(currentUserEmail);

        Page<Activity> activities;
        if (search != null && !search.isBlank()) {
            activities = activityRepository.findByNameContainingIgnoreCase(search, pageable);
        } else if (category != null && !category.isBlank() && !category.equals("All")) {
            activities = activityRepository.findByCategory(category, pageable);
        } else {
            activities = activityRepository.findAll(pageable);
        }

        return activities.map(a -> toResponse(a, currentUserId));
    }

    public ActivityResponse getActivityById(Long id, String currentUserEmail) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + id));
        Long currentUserId = getCurrentUserId(currentUserEmail);
        return toResponse(activity, currentUserId);
    }

    public ActivityResponse createActivity(ActivityRequest request, MultipartFile image) {
        String finalImageUrl = request.getImageUrl();

        if (image != null && !image.isEmpty()) {
            try {
                Path uploadDir = Paths.get("uploads");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                String originalFilename = image.getOriginalFilename();
                String fileExtension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".jpg";
                String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
                Path filePath = uploadDir.resolve(uniqueFilename);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Accessible via statically routed /uploads/
                finalImageUrl = "http://localhost:8086/uploads/" + uniqueFilename;
            } catch (IOException e) {
                throw new BadRequestException("Failed to save uploaded image.");
            }
        }

        Activity activity = Activity.builder()
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .capacity(request.getCapacity())
                .enrolled(0)
                .organizer(request.getOrganizer())
                .schedule(request.getSchedule())
                .location(request.getLocation())
                .imageUrl(finalImageUrl)
                .status(Activity.Status.ACTIVE)
                .build();
        return toResponse(activityRepository.save(activity), null);
    }

    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + id));
        activity.setName(request.getName());
        activity.setCategory(request.getCategory());
        activity.setDescription(request.getDescription());
        activity.setCapacity(request.getCapacity());
        activity.setOrganizer(request.getOrganizer());
        activity.setSchedule(request.getSchedule());
        activity.setLocation(request.getLocation());
        if (request.getImageUrl() != null)
            activity.setImageUrl(request.getImageUrl());
        if (request.getStatus() != null) {
            activity.setStatus(Activity.Status.valueOf(request.getStatus().toUpperCase()));
        }
        return toResponse(activityRepository.save(activity), null);
    }

    public void deleteActivity(Long id) {
        if (!activityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Activity not found with id: " + id);
        }
        activityRepository.deleteById(id);
    }

    public List<ActivityResponse> getAllForAdmin() {
        return activityRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(a -> toResponse(a, null)).toList();
    }

    private Long getCurrentUserId(String email) {
        if (email == null)
            return null;
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    private ActivityResponse toResponse(Activity a, Long currentUserId) {
        boolean enrolled = currentUserId != null &&
                participationRepository.existsByUserIdAndActivityId(currentUserId, a.getId());
        return ActivityResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .category(a.getCategory())
                .description(a.getDescription())
                .capacity(a.getCapacity())
                .enrolled(a.getEnrolled())
                .organizer(a.getOrganizer())
                .schedule(a.getSchedule())
                .location(a.getLocation())
                .status(a.getStatus().name())
                .imageUrl(a.getImageUrl())
                .createdAt(a.getCreatedAt())
                .enrolledByCurrentUser(enrolled)
                .build();
    }
}
