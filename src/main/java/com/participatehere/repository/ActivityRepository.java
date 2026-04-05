package com.participatehere.repository;

import com.participatehere.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByCategory(String category, Pageable pageable);
    Page<Activity> findByStatus(Activity.Status status, Pageable pageable);
    Page<Activity> findByNameContainingIgnoreCase(String name, Pageable pageable);
    long countByStatus(Activity.Status status);
}
