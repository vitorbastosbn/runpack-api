package com.runpack.api.service;

import com.runpack.api.dto.request.CreateGroupRequest;
import com.runpack.api.entity.Group;
import com.runpack.api.entity.GroupMember;
import com.runpack.api.entity.User;
import com.runpack.api.exception.BadRequestException;
import com.runpack.api.exception.PremiumRequiredException;
import com.runpack.api.repository.GroupMemberRepository;
import com.runpack.api.repository.GroupRepository;
import com.runpack.api.repository.InviteTokenRepository;
import com.runpack.api.repository.RunResultRepository;
import com.runpack.api.repository.SessionRepository;
import com.runpack.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupServiceTests {

    private GroupRepository groupRepository;
    private GroupMemberRepository groupMemberRepository;
    private UserRepository userRepository;
    private GroupService service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        userRepository = mock(UserRepository.class);
        service = new GroupService(groupRepository, groupMemberRepository, userRepository,
            mock(SessionRepository.class), mock(RunResultRepository.class),
            mock(InviteTokenRepository.class));

        userId = UUID.randomUUID();
        user = new User();
        user.setName("Tester");
        user.setPlan(User.Plan.free);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupRepository.countMembers(any())).thenReturn(0L);
    }

    private CreateGroupRequest request() {
        return new CreateGroupRequest("Pack", null, null);
    }

    @Test
    void freeUserWithThreeMembershipsCannotCreateGroup() {
        when(groupRepository.countByMemberId(userId)).thenReturn(3L);

        assertThatThrownBy(() -> service.createGroup(userId, request()))
            .isInstanceOf(PremiumRequiredException.class)
            .satisfies(ex -> assertThat(((PremiumRequiredException) ex).getCode())
                .isEqualTo("GROUP_LIMIT_REACHED"));
    }

    @Test
    void freeUserWithTwoMembershipsCanCreateGroup() {
        when(groupRepository.countByMemberId(userId)).thenReturn(2L);

        assertThatCode(() -> service.createGroup(userId, request())).doesNotThrowAnyException();
    }

    @Test
    void premiumUserWithNineMembershipsCanCreateGroup() {
        user.setPlan(User.Plan.premium);
        when(groupRepository.countByMemberId(userId)).thenReturn(9L);

        assertThatCode(() -> service.createGroup(userId, request())).doesNotThrowAnyException();
    }

    @Test
    void premiumUserWithTenMembershipsBlockedByExistingCap() {
        user.setPlan(User.Plan.premium);
        when(groupRepository.countByMemberId(userId)).thenReturn(10L);

        assertThatThrownBy(() -> service.createGroup(userId, request()))
            .isInstanceOf(BadRequestException.class);
    }
}
