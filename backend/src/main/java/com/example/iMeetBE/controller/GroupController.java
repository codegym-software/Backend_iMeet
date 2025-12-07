package com.example.iMeetBE.controller;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.CreateGroupRequest;
import com.example.iMeetBE.dto.GroupResponse;
import com.example.iMeetBE.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    /**
     * Tạo group mới
     * POST /api/groups
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            GroupResponse group = groupService.createGroup(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Tạo group thành công", group));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Lấy thông tin group theo ID
     * GET /api/groups/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroup(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            GroupResponse group = groupService.getGroupById(id, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy thông tin group thành công", group));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Lấy danh sách các group của user hiện tại
     * GET /api/groups/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getMyGroups(Authentication authentication) {
        try {
            String userId = authentication.getName();
            List<GroupResponse> groups = groupService.getMyGroups(userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách group thành công", groups));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Cập nhật thông tin group
     * PUT /api/groups/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            GroupResponse group = groupService.updateGroup(id, request, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật group thành công", group));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Xóa group
     * DELETE /api/groups/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            groupService.deleteGroup(id, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Xóa group thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Xóa thành viên khỏi group
     * DELETE /api/groups/{groupId}/members/{userId}
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long groupId,
            @PathVariable String userId,
            Authentication authentication) {
        try {
            String currentUserId = authentication.getName();
            groupService.removeMember(groupId, userId, currentUserId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Xóa thành viên thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }
}
