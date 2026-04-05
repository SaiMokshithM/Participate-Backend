package com.participatehere.service;

import com.participatehere.dto.response.UserResponse;
import com.participatehere.entity.User;
import com.participatehere.repository.AdminRepository;
import com.participatehere.repository.ParticipationRepository;
import com.participatehere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final ParticipationRepository participationRepository;

    public Map<String, Object> getDashboardStats() {
        long totalStudents = userRepository.countByRole(User.Role.STUDENT);
        long totalAdmins = adminRepository.count();  // admins live in the separate `admins` table
        long totalUsers = userRepository.count();
        long totalParticipations = participationRepository.count();
        return Map.of(
                "totalStudents", totalStudents,
                "totalAdmins", totalAdmins,
                "totalUsers", totalUsers,
                "totalParticipations", totalParticipations
        );
    }

    public Page<UserResponse> getAllStudents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> students = userRepository.findByRole(User.Role.STUDENT, pageable);
        return students.map(this::toUserResponse);
    }

    private UserResponse toUserResponse(User u) {
        long activitiesJoined = participationRepository.countByUserId(u.getId());
        String studentId = u.getStudentId();
        return UserResponse.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .studentId(studentId)
                .email(u.getEmail())
                .role(u.getRole().name())
                .createdAt(u.getCreatedAt())
                .activitiesJoined(activitiesJoined)
                .build();
    }
}
