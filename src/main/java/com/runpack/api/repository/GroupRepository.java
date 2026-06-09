package com.runpack.api.repository;

import com.runpack.api.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    @Query("""
        SELECT g FROM Group g
        JOIN GroupMember gm ON gm.group.id = g.id
        WHERE gm.user.id = :userId
        ORDER BY g.createdAt DESC
        """)
    Page<Group> findAllByMemberId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT g FROM Group g
        JOIN GroupMember gm ON gm.group.id = g.id
        WHERE gm.user.id = :userId
          AND LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY g.createdAt DESC
        """)
    Page<Group> searchByMemberId(@Param("userId") UUID userId, @Param("q") String q, Pageable pageable);

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId")
    long countMembers(@Param("groupId") UUID groupId);

    @Query("""
        SELECT COUNT(g) FROM Group g
        JOIN GroupMember gm ON gm.group.id = g.id
        WHERE gm.user.id = :userId
        """)
    long countByMemberId(@Param("userId") UUID userId);
}
