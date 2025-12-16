package com.example.iMeetBE.service;

import com.example.iMeetBE.dto.GroupInviteResponse;
import com.example.iMeetBE.dto.InviteToGroupRequest;
import com.example.iMeetBE.dto.ValidateInviteResponse;
import com.example.iMeetBE.model.*;
import com.example.iMeetBE.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class GroupInvitationService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupInviteRepository groupInviteRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GroupService groupService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${jwt.secret:your-secret-key-change-this-in-production}")
    private String jwtSecret;

    /**
     * Tạo lời mời vào group và gửi email
     */
    @Transactional
    public GroupInviteResponse createInvitation(InviteToGroupRequest request, String inviterId) {
        // Validate group exists
        Group group = groupRepository.findById(request.getGroupId())
            .orElseThrow(() -> new RuntimeException("Group không tồn tại"));

        // Validate user has permission to invite
        if (!groupService.hasPermissionToInvite(request.getGroupId(), inviterId)) {
            throw new RuntimeException("Bạn không có quyền mời thành viên vào group này");
        }

        User inviter = userRepository.findById(inviterId)
            .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Kiểm tra email đã là thành viên chưa
        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existingUser != null) {
            boolean alreadyMember = groupMemberRepository.existsByGroupAndUser(group, existingUser);
            if (alreadyMember) {
                throw new RuntimeException("User này đã là thành viên của group");
            }
        }

        // Kiểm tra đã có lời mời pending chưa
        boolean hasPendingInvite = groupInviteRepository.existsByGroupAndInvitedEmailAndStatus(
            group, request.getEmail(), InviteStatusGroup.PENDING
        );
        if (hasPendingInvite) {
            throw new RuntimeException("Đã có lời mời pending cho email này");
        }

        // Tạo invite token (JWT hoặc UUID)
        String inviteToken = generateInviteToken(request.getEmail(), group.getId());

        // Parse role
        GroupRole role;
        try {
            role = GroupRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            role = GroupRole.MEMBER;
        }

        // Tạo invite record
        GroupInvite invite = new GroupInvite(group, inviteToken, request.getEmail(), inviter, role);
        invite.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7 ngày
        invite = groupInviteRepository.save(invite);

        // Tạo invite link
        String inviteLink = frontendBaseUrl + "/group/invite/" + inviteToken;

        // Gửi email
        String inviterName = inviter.getFullName() != null ? inviter.getFullName() : inviter.getUsername();
        emailService.sendGroupInvitationEmail(
            request.getEmail(),
            inviterName,
            group.getName(),
            inviteLink,
            request.getMessage()
        );

        // Tạo response
        return new GroupInviteResponse(
            invite.getId(),
            invite.getInviteToken(),
            invite.getInvitedEmail(),
            group.getName(),
            group.getId(),
            inviterName,
            invite.getStatus().name(),
            invite.getInvitedRole().name(),
            invite.getExpiresAt(),
            invite.getCreatedAt(),
            inviteLink
        );
    }

    /**
     * Validate invite token
     */
    public ValidateInviteResponse validateInvite(String token) {
        GroupInvite invite = groupInviteRepository.findByInviteToken(token)
            .orElse(null);

        if (invite == null) {
            ValidateInviteResponse response = new ValidateInviteResponse(false, "Lời mời không tồn tại");
            return response;
        }

        // Kiểm tra trạng thái
        if (invite.getStatus() != InviteStatusGroup.PENDING) {
            ValidateInviteResponse response = new ValidateInviteResponse(
                false, 
                "Lời mời đã " + (invite.getStatus() == InviteStatusGroup.ACCEPTED ? "được chấp nhận" : 
                                 invite.getStatus() == InviteStatusGroup.DECLINED ? "bị từ chối" : "hết hạn")
            );
            return response;
        }

        // Kiểm tra hết hạn
        if (invite.isExpired()) {
            invite.setStatus(InviteStatusGroup.EXPIRED);
            groupInviteRepository.save(invite);
            
            ValidateInviteResponse response = new ValidateInviteResponse(false, "Lời mời đã hết hạn");
            return response;
        }

        // Kiểm tra user đã tồn tại chưa
        boolean userExists = userRepository.findByEmail(invite.getInvitedEmail()).isPresent();

        // Valid invite
        ValidateInviteResponse response = new ValidateInviteResponse(true, "Lời mời hợp lệ");
        response.setGroupName(invite.getGroup().getName());
        response.setGroupId(invite.getGroup().getId());
        response.setInvitedByName(
            invite.getInvitedBy().getFullName() != null ? 
            invite.getInvitedBy().getFullName() : 
            invite.getInvitedBy().getUsername()
        );
        response.setRole(invite.getInvitedRole().name());
        response.setUserExists(userExists);
        response.setInvitedEmail(invite.getInvitedEmail());

        return response;
    }

    /**
     * Accept invitation - tự động join group
     */
    @Transactional
    public GroupInviteResponse acceptInvite(String token, String userId) {
        GroupInvite invite = groupInviteRepository.findByInviteToken(token)
            .orElseThrow(() -> new RuntimeException("Lời mời không tồn tại"));

        // Validate status
        if (invite.getStatus() != InviteStatusGroup.PENDING) {
            throw new RuntimeException("Lời mời không còn hợp lệ");
        }

        // Validate expiry
        if (invite.isExpired()) {
            invite.setStatus(InviteStatusGroup.EXPIRED);
            groupInviteRepository.save(invite);
            throw new RuntimeException("Lời mời đã hết hạn");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Validate email matches (case insensitive và trim whitespace)
        String userEmail = user.getEmail().trim().toLowerCase();
        String invitedEmail = invite.getInvitedEmail().trim().toLowerCase();
        
        // Log để debug
        System.out.println("Validating email - User: '" + userEmail + "' vs Invited: '" + invitedEmail + "'");
        
        if (!userEmail.equals(invitedEmail)) {
            // Strict validation: Email phải khớp chính xác
            throw new RuntimeException("Email không khớp với lời mời. Email của bạn: " + user.getEmail() + ", Email được mời: " + invite.getInvitedEmail());
        }

        Group group = invite.getGroup();

        // Kiểm tra đã là thành viên chưa
        boolean alreadyMember = groupMemberRepository.existsByGroupAndUser(group, user);
        if (alreadyMember) {
            throw new RuntimeException("Bạn đã là thành viên của group này");
        }

        // Thêm vào group
        GroupMember member = new GroupMember(group, user, invite.getInvitedRole());
        groupMemberRepository.save(member);

        // Cập nhật invite status
        invite.setStatus(InviteStatusGroup.ACCEPTED);
        invite.setAcceptedAt(LocalDateTime.now());
        invite = groupInviteRepository.save(invite);

        String inviterName = invite.getInvitedBy().getFullName() != null ? 
            invite.getInvitedBy().getFullName() : 
            invite.getInvitedBy().getUsername();

        return new GroupInviteResponse(
            invite.getId(),
            invite.getInviteToken(),
            invite.getInvitedEmail(),
            group.getName(),
            group.getId(),
            inviterName,
            invite.getStatus().name(),
            invite.getInvitedRole().name(),
            invite.getExpiresAt(),
            invite.getCreatedAt(),
            null
        );
    }

    /**
     * Decline invitation
     */
    @Transactional
    public void declineInvite(String token) {
        GroupInvite invite = groupInviteRepository.findByInviteToken(token)
            .orElseThrow(() -> new RuntimeException("Lời mời không tồn tại"));

        if (invite.getStatus() != InviteStatusGroup.PENDING) {
            throw new RuntimeException("Lời mời không còn hợp lệ");
        }

        invite.setStatus(InviteStatusGroup.DECLINED);
        groupInviteRepository.save(invite);
    }

    /**
     * Revoke invitation (by inviter or admin)
     */
    @Transactional
    public void revokeInvite(Long inviteId, String userId) {
        GroupInvite invite = groupInviteRepository.findById(inviteId)
            .orElseThrow(() -> new RuntimeException("Lời mời không tồn tại"));

        // Kiểm tra quyền: phải là người mời hoặc admin/owner của group
        if (!invite.getInvitedBy().getId().equals(userId)) {
            if (!groupService.hasPermissionToInvite(invite.getGroup().getId(), userId)) {
                throw new RuntimeException("Bạn không có quyền thu hồi lời mời này");
            }
        }

        if (invite.getStatus() != InviteStatusGroup.PENDING) {
            throw new RuntimeException("Chỉ có thể thu hồi lời mời đang pending");
        }

        invite.setStatus(InviteStatusGroup.DECLINED);
        groupInviteRepository.save(invite);
    }

    /**
     * Get all invitations for a group
     */
    public List<GroupInvite> getGroupInvitations(Long groupId, String userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new RuntimeException("Group không tồn tại"));

        // Kiểm tra quyền
        if (!groupService.hasPermissionToInvite(groupId, userId)) {
            throw new RuntimeException("Bạn không có quyền xem danh sách lời mời");
        }

        return groupInviteRepository.findByGroup(group);
    }

    /**
     * Scheduled task để tự động expire các invite đã hết hạn
     */
    @Scheduled(cron = "0 0 * * * *") // Chạy mỗi giờ
    @Transactional
    public void expireOldInvitations() {
        List<GroupInvite> expiredInvites = groupInviteRepository.findExpiredInvites(
            InviteStatusGroup.PENDING, 
            LocalDateTime.now()
        );

        for (GroupInvite invite : expiredInvites) {
            invite.setStatus(InviteStatusGroup.EXPIRED);
        }

        if (!expiredInvites.isEmpty()) {
            groupInviteRepository.saveAll(expiredInvites);
            System.out.println("Expired " + expiredInvites.size() + " group invitations");
        }
    }

    /**
     * Generate invite token using JWT
     */
    private String generateInviteToken(String email, Long groupId) {
        // Có thể dùng JWT hoặc UUID
        // Dùng JWT để có thể verify và extract info
        long expiryTime = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000); // 7 days

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        
        return Jwts.builder()
            .setSubject(email)
            .claim("groupId", groupId)
            .claim("type", "group_invite")
            .setIssuedAt(new Date())
            .setExpiration(new Date(expiryTime))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }
}
