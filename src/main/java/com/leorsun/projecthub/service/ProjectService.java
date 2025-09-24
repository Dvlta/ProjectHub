package com.leorsun.projecthub.service;

import com.leorsun.projecthub.dto.CreateProjectDto;
import com.leorsun.projecthub.dto.InviteRequestDto;
import com.leorsun.projecthub.dto.UpdateProjectDto;
import com.leorsun.projecthub.model.*;
import com.leorsun.projecthub.repository.*;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectInviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository memberRepository,
                          ProjectInviteRepository inviteRepository,
                          UserRepository userRepository,
                          EmailService emailService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public Project createProject(User owner, CreateProjectDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name is required");
        }
        String key = dto.getKey() != null && !dto.getKey().isBlank() ? sanitizeKey(dto.getKey()) : generateKeyFromName(dto.getName());
        if (projectRepository.existsByKeyIgnoreCase(key)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project key already exists");
        }
        Project project = new Project();
        project.setName(dto.getName().trim());
        project.setKey(key);
        project.setDescription(dto.getDescription());
        project.setOwner(owner);
        project = projectRepository.save(project);

        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProject(project);
        ownerMember.setUser(owner);
        ownerMember.setRole(ProjectRole.OWNER);
        memberRepository.save(ownerMember);

        return project;
    }

    public List<Project> listMyProjects(User user) {
        return memberRepository.findByUser_Id(user.getId()).stream()
                .map(ProjectMember::getProject)
                .collect(Collectors.toList());
    }

    public Project getProject(User user, Long projectId) {
        requireMember(user, projectId);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    public Project updateProject(User user, Long projectId, UpdateProjectDto dto) {
        Project project = getProject(user, projectId);
        requireRole(user, projectId, ProjectRole.ADMIN);
        if (dto.getName() != null) project.setName(dto.getName());
        if (dto.getDescription() != null) project.setDescription(dto.getDescription());
        return projectRepository.save(project);
    }

    public void deleteProject(User user, Long projectId) {
        Project project = getProject(user, projectId);
        requireRole(user, projectId, ProjectRole.OWNER);
        projectRepository.delete(project);
    }

    public List<ProjectMember> listMembers(User user, Long projectId) {
        requireMember(user, projectId);
        return memberRepository.findByProject_Id(projectId);
    }

    public void removeMember(User actor, Long projectId, Long userId) {
        requireRole(actor, projectId, ProjectRole.ADMIN);
        ProjectMember m = memberRepository.findByProject_IdAndUser_Id(projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        // Prevent removing the last OWNER
        if (m.getRole() == ProjectRole.OWNER) {
            long owners = memberRepository.countByProject_IdAndRole(projectId, ProjectRole.OWNER);
            if (owners <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the last owner");
            }
        }
        memberRepository.delete(m);
    }

    public ProjectInvite invite(User inviter, Long projectId, InviteRequestDto dto) {
        requireRole(inviter, projectId, ProjectRole.ADMIN);
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (inviteRepository.existsByProject_IdAndEmailIgnoreCase(projectId, dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invite already sent to this email");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        ProjectInvite invite = new ProjectInvite();
        invite.setProject(project);
        invite.setEmail(dto.getEmail().trim());
        invite.setRole(dto.getRole() == null ? ProjectRole.MEMBER : dto.getRole());
        invite.setToken(UUID.randomUUID().toString());
        invite.setInvitedBy(inviter);
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        invite = inviteRepository.save(invite);

        sendInviteEmail(invite);
        return invite;
    }

    public void acceptInvite(User user, String token) {
        ProjectInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));
        if (invite.getAcceptedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already accepted");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite expired");
        }
        // If user already a member, just mark accepted
        if (!memberRepository.existsByProject_IdAndUser_Id(invite.getProject().getId(), user.getId())) {
            ProjectMember m = new ProjectMember();
            m.setProject(invite.getProject());
            m.setUser(user);
            m.setRole(invite.getRole());
            memberRepository.save(m);
        }
        invite.setAcceptedAt(LocalDateTime.now());
        inviteRepository.save(invite);
    }

    private void sendInviteEmail(ProjectInvite invite) {
        String subject = "You're invited to a project";
        String html = "<p>You have been invited to join project <b>" + escape(invite.getProject().getName()) +
                "</b> as <b>" + invite.getRole() + "</b>.</p>" +
                "<p>Use this token to accept: <b>" + invite.getToken() + "</b></p>" +
                "<p>Or call POST /api/invites/" + invite.getToken() + "/accept from the app.</p>";
        try {
            emailService.sendVerificationEmail(invite.getEmail(), subject, html);
        } catch (MessagingException e) {
            // Best-effort email; do not fail invite creation
        }
    }

    private void requireMember(User user, Long projectId) {
        if (!memberRepository.existsByProject_IdAndUser_Id(projectId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a project member");
        }
    }

    private void requireRole(User user, Long projectId, ProjectRole required) {
        ProjectMember member = memberRepository.findByProject_IdAndUser_Id(projectId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a project member"));
        if (!member.getRole().atLeast(required)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
        }
    }

    private String sanitizeKey(String raw) {
        return raw.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private String generateKeyFromName(String name) {
        String base = sanitizeKey(name).replaceAll("\\s+", "");
        if (base.length() > 6) base = base.substring(0, 6);
        String candidate = base.isEmpty() ? "PRJ" : base;
        int suffix = 1;
        while (projectRepository.existsByKeyIgnoreCase(candidate)) {
            candidate = base + suffix++;
            if (candidate.length() > 10) candidate = base.substring(0, Math.max(1, 10 - String.valueOf(suffix - 1).length())) + (suffix - 1);
        }
        return candidate;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("<", "&lt;").replace(">", "&gt;");
    }
}

