package com.example.iMeetBE.controller;

import com.example.iMeetBE.dto.ApiResponse;
import com.example.iMeetBE.dto.GroupInviteResponse;
import com.example.iMeetBE.dto.InviteToGroupRequest;
import com.example.iMeetBE.dto.ValidateInviteResponse;
import com.example.iMeetBE.model.GroupInvite;
import com.example.iMeetBE.service.GroupInvitationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-invites")
public class GroupInvitationController {

    @Autowired
    private GroupInvitationService invitationService;

    /**
     * Tạo lời mời vào group
     * POST /api/group-invites
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GroupInviteResponse>> createInvitation(
            @Valid @RequestBody InviteToGroupRequest request,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            GroupInviteResponse invite = invitationService.createInvitation(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Gửi lời mời thành công", invite));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Validate invite token (public endpoint - không cần authentication)
     * GET /api/group-invites/validate/{token}
     */
    @GetMapping("/validate/{token}")
    public ResponseEntity<ApiResponse<ValidateInviteResponse>> validateInvite(@PathVariable String token) {
        try {
            ValidateInviteResponse response = invitationService.validateInvite(token);
            if (response.isValid()) {
                return ResponseEntity.ok(new ApiResponse<>(true, response.getMessage(), response));
            } else {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, response.getMessage(), response));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Chấp nhận lời mời và tự động join group
     * POST /api/group-invites/accept/{token}
     */
    @PostMapping("/accept/{token}")
    public ResponseEntity<ApiResponse<GroupInviteResponse>> acceptInvite(
            @PathVariable String token,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            GroupInviteResponse response = invitationService.acceptInvite(token, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã tham gia group thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Từ chối lời mời (có thể public hoặc yêu cầu auth tùy business logic)
     * POST /api/group-invites/decline/{token}
     */
    @PostMapping("/decline/{token}")
    public ResponseEntity<ApiResponse<Void>> declineInvite(@PathVariable String token) {
        try {
            invitationService.declineInvite(token);
            return ResponseEntity.ok(new ApiResponse<>(true, "Đã từ chối lời mời"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Thu hồi lời mời (chỉ người mời hoặc admin/owner)
     * DELETE /api/group-invites/{inviteId}
     */
    @DeleteMapping("/{inviteId}")
    public ResponseEntity<ApiResponse<Void>> revokeInvite(
            @PathVariable Long inviteId,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            invitationService.revokeInvite(inviteId, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Thu hồi lời mời thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    /**
     * Lấy danh sách lời mời của một group
     * GET /api/group-invites/group/{groupId}
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<GroupInvite>>> getGroupInvitations(
            @PathVariable Long groupId,
            Authentication authentication) {
        try {
            String userId = authentication.getName();
            List<GroupInvite> invites = invitationService.getGroupInvitations(groupId, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách lời mời thành công", invites));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage()));
        }
    }
}
