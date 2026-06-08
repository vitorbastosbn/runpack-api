package com.runpack.api.repository;

import com.runpack.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
}
