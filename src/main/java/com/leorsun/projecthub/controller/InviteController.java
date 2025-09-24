package com.leorsun.projecthub.controller;

import com.leorsun.projecthub.model.User;
import com.leorsun.projecthub.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invites")
public class InviteController {
    private final ProjectService projectService;

    public InviteController(ProjectService projectService) {
        this.projectService = projectService;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<?> accept(@PathVariable String token) {
        projectService.acceptInvite(currentUser(), token);
        return ResponseEntity.ok().build();
    }
}

