package com.carlosdaza.splitexpense.service;

import com.carlosdaza.splitexpense.domain.dto.SplitDto;
import com.carlosdaza.splitexpense.domain.entity.Group;
import com.carlosdaza.splitexpense.domain.entity.User;
import com.carlosdaza.splitexpense.exception.BusinessException;
import com.carlosdaza.splitexpense.exception.ResourceNotFoundException;
import com.carlosdaza.splitexpense.repository.GroupRepository;
import com.carlosdaza.splitexpense.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    @Transactional
    public SplitDto.GroupResponse create(SplitDto.GroupRequest request, UUID creatorId) {
        User creator = findUserOrThrow(creatorId);
        Group group = Group.builder()
                .name(request.name())
                .description(request.description())
                .createdBy(creator)
                .build();
        group.getMembers().add(creator);
        return toResponse(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public SplitDto.GroupResponse getById(UUID groupId) {
        return toResponse(findGroupOrThrow(groupId));
    }

    @Transactional(readOnly = true)
    public List<SplitDto.GroupResponse> getMyGroups(UUID userId) {
        return groupRepository.findByMemberId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SplitDto.GroupResponse addMember(UUID groupId, UUID userId, UUID requesterId) {
        Group group = findGroupOrThrow(groupId);
        if (!group.getCreatedBy().getId().equals(requesterId)) {
            throw new BusinessException("Only the group creator can add members.");
        }
        User user = findUserOrThrow(userId);
        if (group.getMembers().contains(user)) {
            throw new BusinessException("User is already a member of this group.");
        }
        group.getMembers().add(user);
        return toResponse(groupRepository.save(group));
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId, UUID requesterId) {
        Group group = findGroupOrThrow(groupId);
        if (!group.getCreatedBy().getId().equals(requesterId)) {
            throw new BusinessException("Only the group creator can remove members.");
        }
        if (userId.equals(group.getCreatedBy().getId())) {
            throw new BusinessException("Cannot remove the group creator.");
        }
        User user = findUserOrThrow(userId);
        group.getMembers().remove(user);
        groupRepository.save(group);
    }

    public Group findGroupOrThrow(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupId.toString()));
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    private SplitDto.GroupResponse toResponse(Group g) {
        List<SplitDto.UserResponse> members = g.getMembers().stream()
                .map(u -> SplitDto.UserResponse.builder()
                        .id(u.getId()).name(u.getName())
                        .email(u.getEmail()).createdAt(u.getCreatedAt()).build())
                .toList();

        return SplitDto.GroupResponse.builder()
                .id(g.getId()).name(g.getName()).description(g.getDescription())
                .createdBy(g.getCreatedBy().getId()).createdByName(g.getCreatedBy().getName())
                .members(members).createdAt(g.getCreatedAt())
                .build();
    }
}
