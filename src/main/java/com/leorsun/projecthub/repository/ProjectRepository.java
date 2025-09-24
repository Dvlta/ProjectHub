package com.leorsun.projecthub.repository;

import com.leorsun.projecthub.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByKeyIgnoreCase(String key);
    Optional<Project> findByKeyIgnoreCase(String key);
}

