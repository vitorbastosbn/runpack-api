package com.runpack.api.repository;

import com.runpack.api.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByGroup_IdAndUser_Id(UUID groupId, UUID userId);

    List<GroupMember> findByGroup_IdOrderByJoinedAtAsc(UUID groupId);

    long countByGroup_IdAndRole(UUID groupId, GroupMember.Role role);

    boolean existsByGroup_IdAndUser_Id(UUID groupId, UUID userId);

    @Query("""
        SELECT COUNT(gm1) > 0 FROM GroupMember gm1
        JOIN GroupMember gm2 ON gm2.group.id = gm1.group.id
        WHERE gm1.user.id = :userId1 AND gm2.user.id = :userId2
    """)
    boolean haveGroupInCommon(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    List<GroupMember> findByUserId(UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM GroupMember gm WHERE gm.group.id = :groupId")
    void deleteAllByGroupId(@Param("groupId") UUID groupId);
}
