package com.leorsun.projecthub.repository;

import com.leorsun.projecthub.model.ProjectInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, Long> {
    Optional<ProjectInvite> findByToken(String token);
    boolean existsByProject_IdAndEmailIgnoreCase(Long projectId, String email);
}

