package com.runpack.api.repository;

import com.runpack.api.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByGroup_IdAndUser_Id(UUID groupId, UUID userId);

    List<GroupMember> findByGroup_IdOrderByJoinedAtAsc(UUID groupId);

    long countByGroup_IdAndRole(UUID groupId, GroupMember.Role role);

    boolean existsByGroup_IdAndUser_Id(UUID groupId, UUID userId);
}
