package com.runpack.api.repository;

import com.runpack.api.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    Optional<Achievement> findBySlug(String slug);
}
