package com.runpack.api.service;

import com.runpack.api.dto.request.CreateGroupRequest;
import com.runpack.api.dto.request.UpdateGroupRequest;
import com.runpack.api.dto.response.GroupLastRunResponse;
import com.runpack.api.dto.response.GroupMemberResponse;
import com.runpack.api.dto.response.GroupResponse;
import com.runpack.api.dto.response.GroupRunSummaryResponse;
import com.runpack.api.entity.Group;
import com.runpack.api.entity.GroupMember;
import com.runpack.api.entity.RunResult;
import com.runpack.api.entity.User;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.exception.ConflictException;
import com.runpack.api.exception.ForbiddenException;
import com.runpack.api.exception.NotFoundException;
import com.runpack.api.entity.Session;
import com.runpack.api.repository.GroupMemberRepository;
import com.runpack.api.repository.GroupRepository;
import com.runpack.api.repository.RunResultRepository;
import com.runpack.api.repository.SessionRepository;
import com.runpack.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GroupService {

    private static final int MAX_MEMBERS = 50;
    private static final int MAX_GROUPS_PER_USER = 10;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final RunResultRepository runResultRepository;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository,
                        SessionRepository sessionRepository,
                        RunResultRepository runResultRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.runResultRepository = runResultRepository;
    }

    /**
     * Top 3 of the group's most recent finished session. Returns null if the group has
     * no finished sessions yet (controller maps that to 204 No Content).
     */
    public GroupLastRunResponse getLastRun(UUID groupId, UUID currentUserId) {
        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, currentUserId)) {
            throw new ForbiddenException("Você não é membro deste grupo");
        }
        Session session = sessionRepository
            .findTopByGroupIdAndStatusOrderByFinishedAtDesc(groupId, Session.Status.finished)
            .orElse(null);
        if (session == null) {
            return null;
        }
        List<GroupLastRunResponse.PodiumEntry> podium = runResultRepository
            .findBySessionIdOrderByFinalRankAsc(session.getId()).stream()
            .limit(3)
            .map(this::toPodiumEntry)
            .toList();
        if (podium.isEmpty()) {
            return null;
        }
        return new GroupLastRunResponse(session.getId().toString(), session.getFinishedAt(), podium);
    }

    /** Finished runs of the group, newest first. Each item carries the winner summary. */
    public List<GroupRunSummaryResponse> getRuns(UUID groupId, UUID currentUserId) {
        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, currentUserId)) {
            throw new ForbiddenException("Você não é membro deste grupo");
        }
        return sessionRepository
            .findByGroupIdAndStatusOrderByFinishedAtDesc(groupId, Session.Status.finished).stream()
            .map(this::toRunSummary)
            .toList();
    }

    private GroupRunSummaryResponse toRunSummary(Session session) {
        List<RunResult> results = runResultRepository.findBySessionIdOrderByFinalRankAsc(session.getId());
        RunResult winner = results.isEmpty() ? null : results.get(0);
        return new GroupRunSummaryResponse(
            session.getId().toString(),
            session.getFinishedAt(),
            results.size(),
            session.getDistanceGoalM(),
            winner != null ? winner.getUser().getName() : null,
            winner != null ? winner.getUser().getUsername() : null,
            winner != null ? winner.getUser().getAvatarUrl() : null,
            winner != null ? winner.getTotalDistanceM() : null
        );
    }

    private GroupLastRunResponse.PodiumEntry toPodiumEntry(RunResult r) {
        User u = r.getUser();
        return new GroupLastRunResponse.PodiumEntry(
            u.getId().toString(),
            u.getName(),
            u.getUsername(),
            u.getAvatarUrl(),
            r.getTotalDistanceM(),
            r.getFinalRank()
        );
    }

    public Page<GroupResponse> getGroups(UUID userId, String query, Pageable pageable) {
        String q = (query != null && !query.isBlank()) ? query.trim() : null;
        var page = (q == null)
            ? groupRepository.findAllByMemberId(userId, pageable)
            : groupRepository.searchByMemberId(userId, q, pageable);
        return page.map(g -> toResponse(g, userId));
    }

    @Transactional
    public GroupResponse createGroup(UUID userId, CreateGroupRequest request) {
        if (groupRepository.countByMemberId(userId) >= MAX_GROUPS_PER_USER) {
            throw new BadRequestException("Limite de " + MAX_GROUPS_PER_USER + " grupos atingido");
        }
        User creator = findUser(userId);
        Group group = new Group();
        group.setName(request.name());
        group.setDescription(request.description());
        group.setImageUrl(request.imageUrl());
        group.setCreator(creator);
        group = groupRepository.save(group);

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(creator);
        member.setRole(GroupMember.Role.admin);
        groupMemberRepository.save(member);

        return toResponse(group, userId);
    }

    public GroupResponse getGroup(UUID groupId, UUID userId) {
        Group group = findGroup(groupId);
        requireMember(groupId, userId);
        return toResponse(group, userId);
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, UUID userId, UpdateGroupRequest request) {
        Group group = findGroup(groupId);
        requireAdmin(groupId, userId);
        if (request.name() != null && !request.name().isBlank()) group.setName(request.name());
        if (request.description() != null) group.setDescription(request.description());
        if (request.imageUrl() != null) group.setImageUrl(request.imageUrl());
        return toResponse(group, userId);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID userId) {
        findGroup(groupId);
        requireAdmin(groupId, userId);
        groupRepository.deleteById(groupId);
    }

    public List<GroupMemberResponse> getMembers(UUID groupId, UUID userId) {
        findGroup(groupId);
        requireMember(groupId, userId);
        return groupMemberRepository.findByGroup_IdOrderByJoinedAtAsc(groupId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public GroupMemberResponse addMember(UUID groupId, UUID userId, UUID targetUserId) {
        findGroup(groupId);
        requireAdmin(groupId, userId);
        if (groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, targetUserId)) {
            throw new ConflictException("Usuário já é membro do grupo");
        }
        if (groupRepository.countMembers(groupId) >= MAX_MEMBERS) {
            throw new BadRequestException("Limite de " + MAX_MEMBERS + " membros atingido");
        }
        User target = findUser(targetUserId);
        Group group = findGroup(groupId);
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(target);
        member.setRole(GroupMember.Role.member);
        return toMemberResponse(groupMemberRepository.save(member));
    }

    @Transactional
    public void removeMember(UUID groupId, UUID actorId, UUID targetUserId) {
        findGroup(groupId);
        if (actorId.equals(targetUserId)) {
            leaveGroup(groupId, actorId);
            return;
        }
        requireAdmin(groupId, actorId);
        GroupMember target = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Membro não encontrado"));
        groupMemberRepository.delete(target);
    }

    @Transactional
    public GroupMemberResponse updateMemberRole(UUID groupId, UUID actorId, UUID targetUserId, String roleStr) {
        findGroup(groupId);
        requireAdmin(groupId, actorId);
        GroupMember target = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Membro não encontrado"));
        GroupMember.Role newRole = switch (roleStr) {
            case "admin" -> GroupMember.Role.admin;
            case "member" -> GroupMember.Role.member;
            default -> throw new BadRequestException("Role inválido: " + roleStr);
        };
        target.setRole(newRole);
        return toMemberResponse(target);
    }

    private void leaveGroup(UUID groupId, UUID userId) {
        GroupMember member = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new NotFoundException("Você não é membro deste grupo"));
        if (member.getRole() == GroupMember.Role.admin) {
            long adminCount = groupMemberRepository.countByGroup_IdAndRole(groupId, GroupMember.Role.admin);
            if (adminCount <= 1) {
                throw new BadRequestException("Transfira o papel de admin antes de sair");
            }
        }
        groupMemberRepository.delete(member);
    }

    private GroupMember requireAdmin(UUID groupId, UUID userId) {
        GroupMember member = groupMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("Acesso negado"));
        if (member.getRole() != GroupMember.Role.admin) {
            throw new ForbiddenException("Apenas admins podem realizar esta ação");
        }
        return member;
    }

    private void requireMember(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
            throw new ForbiddenException("Acesso negado");
        }
    }

    private Group findGroup(UUID id) {
        return groupRepository.findById(id).orElseThrow(() -> new NotFoundException("Grupo não encontrado"));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private GroupResponse toResponse(Group g, UUID currentUserId) {
        String myRole = groupMemberRepository.findByGroup_IdAndUser_Id(g.getId(), currentUserId)
                .map(m -> m.getRole().name())
                .orElse("none");
        int memberCount = (int) groupRepository.countMembers(g.getId());
        String activeSessionId = sessionRepository.findByGroupIdAndStatus(g.getId(), Session.Status.active)
                .map(s -> s.getId().toString())
                .orElse(null);
        return new GroupResponse(g.getId(), g.getName(), g.getDescription(), g.getImageUrl(), memberCount, myRole, g.getCreatedAt(), activeSessionId);
    }

    private GroupMemberResponse toMemberResponse(GroupMember m) {
        User u = m.getUser();
        return new GroupMemberResponse(m.getId(), u.getId(), u.getName(), u.getUsername(), u.getAvatarUrl(), m.getRole().name(), m.getJoinedAt());
    }
}
