package com.leorsun.projecthub.controller;

import com.leorsun.projecthub.dto.CreateProjectDto;
import com.leorsun.projecthub.dto.InviteRequestDto;
import com.leorsun.projecthub.dto.UpdateProjectDto;
import com.leorsun.projecthub.model.Project;
import com.leorsun.projecthub.model.ProjectInvite;
import com.leorsun.projecthub.model.ProjectMember;
import com.leorsun.projecthub.model.User;
import com.leorsun.projecthub.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    @PostMapping
    public ResponseEntity<Project> create(@RequestBody CreateProjectDto dto) {
        return ResponseEntity.ok(projectService.createProject(currentUser(), dto));
    }

    @GetMapping
    public ResponseEntity<List<Project>> mine() {
        return ResponseEntity.ok(projectService.listMyProjects(currentUser()));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Project> get(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProject(currentUser(), projectId));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<Project> update(@PathVariable Long projectId, @RequestBody UpdateProjectDto dto) {
        return ResponseEntity.ok(projectService.updateProject(currentUser(), projectId, dto));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> delete(@PathVariable Long projectId) {
        projectService.deleteProject(currentUser(), projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMember>> members(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.listMembers(currentUser(), projectId));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        projectService.removeMember(currentUser(), projectId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/invites")
    public ResponseEntity<ProjectInvite> invite(@PathVariable Long projectId, @RequestBody InviteRequestDto dto) {
        return ResponseEntity.ok(projectService.invite(currentUser(), projectId, dto));
    }
}

