package com.runpack.api.repository;

import com.runpack.api.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {
    Optional<InviteToken> findByTokenHash(String tokenHash);
    void deleteByTypeAndTargetId(InviteToken.Type type, UUID targetId);
}
