package com.leorsun.projecthub.repository;

import com.leorsun.projecthub.model.Task;
import com.leorsun.projecthub.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject_IdOrderByOrderIndexAsc(Long projectId);
    List<Task> findByProject_IdAndStatusOrderByOrderIndexAsc(Long projectId, TaskStatus status);
}

