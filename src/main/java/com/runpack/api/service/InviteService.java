package com.runpack.api.service;

import com.runpack.api.dto.request.CreateInviteRequest;
import com.runpack.api.dto.response.AcceptInviteResponse;
import com.runpack.api.dto.response.InviteInfoResponse;
import com.runpack.api.dto.response.InviteResponse;
import com.runpack.api.entity.Group;
import com.runpack.api.entity.GroupMember;
import com.runpack.api.entity.InviteToken;
import com.runpack.api.entity.User;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.exception.ConflictException;
import com.runpack.api.exception.ForbiddenException;
import com.runpack.api.exception.GoneException;
import com.runpack.api.exception.NotFoundException;
import com.runpack.api.exception.PremiumRequiredException;
import com.runpack.api.repository.GroupMemberRepository;
import com.runpack.api.repository.GroupRepository;
import com.runpack.api.repository.InviteTokenRepository;
import com.runpack.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InviteService {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InviteTokenRepository inviteTokenRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public InviteService(InviteTokenRepository inviteTokenRepository,
                         GroupRepository groupRepository,
                         GroupMemberRepository groupMemberRepository,
                         UserRepository userRepository) {
        this.inviteTokenRepository = inviteTokenRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public InviteResponse createInvite(UUID userId, CreateInviteRequest request) {
        InviteToken.Type type = switch (request.type()) {
            case "group" -> InviteToken.Type.group;
            case "session" -> InviteToken.Type.session;
            default -> throw new BadRequestException("Tipo inválido: " + request.type());
        };

        if (type == InviteToken.Type.group) {
            if (!groupMemberRepository.existsByGroup_IdAndUser_Id(request.targetId(), userId)) {
                throw new ForbiddenException("Você não é membro deste grupo");
            }
        }

        User creator = findUser(userId);
        String token = generateToken();
        String tokenHash = sha256(token);
        Instant expiresAt = type == InviteToken.Type.group
                ? Instant.now().plus(24, ChronoUnit.HOURS)
                : Instant.now().plus(30, ChronoUnit.MINUTES);

        InviteToken invite = new InviteToken();
        invite.setTokenHash(tokenHash);
        invite.setType(type);
        invite.setTargetId(request.targetId());
        invite.setCreatedBy(creator);
        invite.setExpiresAt(expiresAt);
        inviteTokenRepository.save(invite);

        String url = "runpack://invite/" + token;
        return new InviteResponse(token, url, expiresAt);
    }

    public InviteInfoResponse getInviteInfo(String token) {
        InviteToken invite = findAndValidate(token);

        String targetName = resolveTargetName(invite);
        User creator = invite.getCreatedBy();

        return new InviteInfoResponse(
                invite.getType().name(),
                invite.getTargetId(),
                targetName,
                new InviteInfoResponse.InvitedBy(creator.getName(), creator.getUsername()),
                invite.getExpiresAt()
        );
    }

    @Transactional
    public AcceptInviteResponse acceptInvite(String token, UUID userId) {
        InviteToken invite = findAndValidate(token);

        if (invite.getType() == InviteToken.Type.group) {
            UUID groupId = invite.getTargetId();
            if (groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
                throw new ConflictException("Você já é membro deste grupo");
            }
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new NotFoundException("Grupo não encontrado"));
            User user = findUser(userId);
            long memberships = groupRepository.countByMemberId(userId);
            if (!user.isPremium() && memberships >= 3) {
                throw new PremiumRequiredException("GROUP_LIMIT_REACHED",
                    "Plano gratuito permite participar de até 3 grupos");
            }
            if (memberships >= 10) {
                throw new BadRequestException("Limite de 10 grupos atingido");
            }
            GroupMember member = new GroupMember();
            member.setGroup(group);
            member.setUser(user);
            member.setRole(GroupMember.Role.member);
            groupMemberRepository.save(member);
        }

        invite.setUsedAt(Instant.now());
        return new AcceptInviteResponse(invite.getType().name(), invite.getTargetId());
    }

    private InviteToken findAndValidate(String token) {
        String hash = sha256(token);
        InviteToken invite = inviteTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new NotFoundException("Link inválido"));
        if (invite.isUsed()) throw new ConflictException("Link já utilizado");
        if (invite.isExpired()) throw new GoneException("Link expirado");
        return invite;
    }

    private String resolveTargetName(InviteToken invite) {
        if (invite.getType() == InviteToken.Type.group) {
            return groupRepository.findById(invite.getTargetId())
                    .map(Group::getName)
                    .orElse("Grupo");
        }
        return "Corrida";
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        BigInteger num = new BigInteger(1, bytes);
        BigInteger base = BigInteger.valueOf(62);
        StringBuilder sb = new StringBuilder();
        while (num.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = num.divideAndRemainder(base);
            sb.insert(0, BASE62.charAt(divRem[1].intValue()));
            num = divRem[0];
        }
        return sb.toString();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
