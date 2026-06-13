package com.carlosdaza.splitexpense.controller;

import com.carlosdaza.splitexpense.config.AuthHelper;
import com.carlosdaza.splitexpense.domain.dto.SplitDto;
import com.carlosdaza.splitexpense.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Create and manage expense groups")
@SecurityRequirement(name = "bearerAuth")
public class GroupController {

    private final GroupService groupService;
    private final AuthHelper authHelper;

    @PostMapping
    @Operation(summary = "Create a new group")
    public ResponseEntity<SplitDto.GroupResponse> create(
            @Valid @RequestBody SplitDto.GroupRequest request) {
        UUID userId = authHelper.getCurrentUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.create(request, userId));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get group by ID")
    public ResponseEntity<SplitDto.GroupResponse> getById(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getById(groupId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all groups I belong to")
    public ResponseEntity<List<SplitDto.GroupResponse>> getMyGroups() {
        UUID userId = authHelper.getCurrentUser().getId();
        return ResponseEntity.ok(groupService.getMyGroups(userId));
    }

    @PostMapping("/{groupId}/members/{userId}")
    @Operation(summary = "Add a member to the group")
    public ResponseEntity<SplitDto.GroupResponse> addMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId) {
        UUID requesterId = authHelper.getCurrentUser().getId();
        return ResponseEntity.ok(groupService.addMember(groupId, userId, requesterId));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @Operation(summary = "Remove a member from the group")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID userId) {
        UUID requesterId = authHelper.getCurrentUser().getId();
        groupService.removeMember(groupId, userId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
