package com.example.iMeetBE.service;

import com.example.iMeetBE.dto.CreateGroupRequest;
import com.example.iMeetBE.dto.GroupResponse;
import com.example.iMeetBE.model.*;
import com.example.iMeetBE.repository.GroupMemberRepository;
import com.example.iMeetBE.repository.GroupRepository;
import com.example.iMeetBE.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, String userId) {
        User owner = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Tạo group mới
        Group group = new Group(request.getName(), request.getDescription(), owner);
        group = groupRepository.save(group);

        // Tự động thêm owner vào group với role OWNER
        GroupMember ownerMember = new GroupMember(group, owner, GroupRole.OWNER);
        groupMemberRepository.save(ownerMember);

        return mapToGroupResponse(group, true);
    }

    public GroupResponse getGroupById(Long groupId, String userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group không tồn tại"));

        // Kiểm tra user có phải là thành viên không
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        
        boolean isMember = groupMemberRepository.existsByGroupAndUser(group, user);
        if (!isMember) {
            throw new RuntimeException("Bạn không có quyền xem group này");
        }

        return mapToGroupResponse(group, true);
    }

    public List<GroupResponse> getMyGroups(String userId) {
        List<Group> groups = groupRepository.findGroupsByUserId(userId);
        return groups.stream()
            .map(g -> mapToGroupResponse(g, false))
            .collect(Collectors.toList());
    }

    @Transactional
    public GroupResponse updateGroup(Long groupId, CreateGroupRequest request, String userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group không tồn tại"));

        // Chỉ owner mới được update
        if (!group.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Chỉ owner mới có quyền cập nhật group");
        }

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group = groupRepository.save(group);

        return mapToGroupResponse(group, true);
    }

    @Transactional
    public void deleteGroup(Long groupId, String userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group không tồn tại"));

        // Chỉ owner mới được xóa
        if (!group.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Chỉ owner mới có quyền xóa group");
        }

        groupRepository.delete(group);
    }

    @Transactional
    public void removeMember(Long groupId, String userIdToRemove, String currentUserId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group không tồn tại"));

        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        User userToRemove = userRepository.findById(userIdToRemove)
            .orElseThrow(() -> new RuntimeException("User cần xóa không tồn tại"));

        // Kiểm tra quyền: owner hoặc admin mới có thể xóa thành viên
        GroupMember currentMember = groupMemberRepository.findByGroupAndUser(group, currentUser)
            .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của group"));

        if (currentMember.getRole() != GroupRole.OWNER && currentMember.getRole() != GroupRole.ADMIN) {
            throw new RuntimeException("Bạn không có quyền xóa thành viên");
        }

        // Không thể xóa owner
        if (group.getOwner().getId().equals(userIdToRemove)) {
            throw new RuntimeException("Không thể xóa owner khỏi group");
        }

        groupMemberRepository.deleteByGroupAndUser(group, userToRemove);
    }

    // Helper method
    private GroupResponse mapToGroupResponse(Group group, boolean includeMembers) {
        int memberCount = (int) groupMemberRepository.countByGroup(group);
        
        GroupResponse response = new GroupResponse(
            group.getId(),
            group.getName(),
            group.getDescription(),
            group.getOwner().getFullName() != null ? group.getOwner().getFullName() : group.getOwner().getUsername(),
            group.getOwner().getId(),
            memberCount,
            group.getCreatedAt(),
            group.getUpdatedAt()
        );

        if (includeMembers) {
            List<GroupMember> members = groupMemberRepository.findByGroup(group);
            List<GroupResponse.GroupMemberDTO> memberDTOs = members.stream()
                .map(m -> new GroupResponse.GroupMemberDTO(
                    m.getUser().getId(),
                    m.getUser().getUsername(),
                    m.getUser().getEmail(),
                    m.getUser().getFullName(),
                    m.getRole().name(),
                    m.getJoinedAt()
                ))
                .collect(Collectors.toList());
            response.setMembers(memberDTOs);
        }

        return response;
    }

    public boolean hasPermissionToInvite(Long groupId, String userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group không tồn tại"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, user)
            .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của group"));

        // Chỉ OWNER và ADMIN mới có quyền mời
        return member.getRole() == GroupRole.OWNER || member.getRole() == GroupRole.ADMIN;
    }
}
