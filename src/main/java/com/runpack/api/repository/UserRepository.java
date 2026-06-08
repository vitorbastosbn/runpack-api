package com.runpack.api.repository;

import com.runpack.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);

    @Query("""
        SELECT u FROM User u
        WHERE u.id <> :currentUserId
          AND LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY u.username
        LIMIT 20
        """)
    List<User> searchByUsername(@Param("query") String query, @Param("currentUserId") UUID currentUserId);
}
