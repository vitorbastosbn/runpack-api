package com.runpack.api.controller;

import com.runpack.api.dto.request.AddMemberRequest;
import com.runpack.api.dto.request.CreateGroupRequest;
import com.runpack.api.dto.request.UpdateGroupRequest;
import com.runpack.api.dto.request.UpdateMemberRoleRequest;
import com.runpack.api.dto.response.GroupMemberResponse;
import com.runpack.api.dto.response.GroupResponse;
import com.runpack.api.security.CurrentUser;
import com.runpack.api.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroups(@CurrentUser UUID userId) {
        return ResponseEntity.ok(groupService.getGroups(userId));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@CurrentUser UUID userId,
                                                      @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.ok(groupService.createGroup(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable UUID id, @CurrentUser UUID userId) {
        return ResponseEntity.ok(groupService.getGroup(id, userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroup(@PathVariable UUID id,
                                                      @CurrentUser UUID userId,
                                                      @Valid @RequestBody UpdateGroupRequest request) {
        return ResponseEntity.ok(groupService.updateGroup(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id, @CurrentUser UUID userId) {
        groupService.deleteGroup(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<GroupMemberResponse>> getMembers(@PathVariable UUID id, @CurrentUser UUID userId) {
        return ResponseEntity.ok(groupService.getMembers(id, userId));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupMemberResponse> addMember(@PathVariable UUID id,
                                                          @CurrentUser UUID userId,
                                                          @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(groupService.addMember(id, userId, request.userId()));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id,
                                              @PathVariable UUID userId,
                                              @CurrentUser UUID currentUserId) {
        groupService.removeMember(id, currentUserId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{userId}")
    public ResponseEntity<GroupMemberResponse> updateMemberRole(@PathVariable UUID id,
                                                                 @PathVariable UUID userId,
                                                                 @CurrentUser UUID currentUserId,
                                                                 @Valid @RequestBody UpdateMemberRoleRequest request) {
        return ResponseEntity.ok(groupService.updateMemberRole(id, currentUserId, userId, request.role()));
    }
}
