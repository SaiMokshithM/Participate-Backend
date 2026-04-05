package com.participatehere.repository;

import com.participatehere.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    List<Participation> findByUserId(Long userId);

    List<Participation> findByActivityId(Long activityId);

    Optional<Participation> findByUserIdAndActivityId(Long userId, Long activityId);

    boolean existsByUserIdAndActivityId(Long userId, Long activityId);

    long countByUserId(Long userId);

    @Query("SELECT p FROM Participation p JOIN FETCH p.activity WHERE p.user.id = :userId")
    List<Participation> findByUserIdWithActivity(@Param("userId") Long userId);

    @Query("SELECT p FROM Participation p JOIN FETCH p.user WHERE p.activity.id = :activityId")
    List<Participation> findByActivityIdWithUser(@Param("activityId") Long activityId);
}
