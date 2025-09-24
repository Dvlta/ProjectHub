package com.leorsun.projecthub.repository;

import com.leorsun.projecthub.model.ProjectMember;
import com.leorsun.projecthub.model.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);
    Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);
    List<ProjectMember> findByProject_Id(Long projectId);
    List<ProjectMember> findByUser_Id(Long userId);
    long countByProject_IdAndRole(Long projectId, ProjectRole role);
}

