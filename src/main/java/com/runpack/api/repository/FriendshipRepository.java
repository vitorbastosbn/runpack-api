package com.runpack.api.repository;

import com.runpack.api.entity.Friendship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    List<Friendship> findByRequester_IdAndStatus(UUID requesterId, Friendship.Status status);

    List<Friendship> findByAddressee_IdAndStatus(UUID addresseeId, Friendship.Status status);

    Page<Friendship> findByAddressee_IdAndStatus(UUID addresseeId, Friendship.Status status, Pageable pageable);

    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
          AND f.status = :status
        ORDER BY f.createdAt DESC
        """)
    Page<Friendship> findFriends(@Param("userId") UUID userId,
                                 @Param("status") Friendship.Status status,
                                 Pageable pageable);

    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :a AND f.addressee.id = :b)
           OR (f.requester.id = :b AND f.addressee.id = :a)
        """)
    Optional<Friendship> findBetween(@Param("a") UUID a, @Param("b") UUID b);

    boolean existsByRequester_IdAndAddressee_Id(UUID requesterId, UUID addresseeId);

    @Query("""
        SELECT CASE WHEN f.requester.id = :userId
                    THEN f.addressee.id
                    ELSE f.requester.id END
        FROM Friendship f
        WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
          AND f.status = com.runpack.api.entity.Friendship.Status.accepted
        """)
    List<UUID> findFriendIds(@Param("userId") UUID userId);

    @Query("""
        SELECT CASE WHEN f.requester.id = :followedUserId
                    THEN f.addressee.id
                    ELSE f.requester.id END
        FROM Friendship f
        WHERE (f.requester.id = :followedUserId OR f.addressee.id = :followedUserId)
          AND f.status = com.runpack.api.entity.Friendship.Status.accepted
          AND (
              (f.requester.id = :followedUserId AND f.addresseeFavorite = true)
              OR
              (f.addressee.id = :followedUserId AND f.requesterFavorite = true)
          )
        """)
    List<UUID> findFavoriteFriendIdsFollowingUser(@Param("followedUserId") UUID followedUserId);

    @Query("""
        SELECT CASE WHEN f.requester.id = :userId
                    THEN f.addressee.id
                    ELSE f.requester.id END
        FROM Friendship f
        WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
          AND f.status = com.runpack.api.entity.Friendship.Status.accepted
          AND (
              (f.requester.id = :userId AND f.requesterFavorite = true)
              OR
              (f.addressee.id = :userId AND f.addresseeFavorite = true)
          )
        """)
    List<UUID> findFavoriteFriendIdsForUser(@Param("userId") UUID userId);
}
