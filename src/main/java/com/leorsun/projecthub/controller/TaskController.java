package com.leorsun.projecthub.controller;

import com.leorsun.projecthub.dto.AssignTaskDto;
import com.leorsun.projecthub.dto.CreateTaskDto;
import com.leorsun.projecthub.dto.MoveTaskDto;
import com.leorsun.projecthub.dto.UpdateTaskDto;
import com.leorsun.projecthub.model.Task;
import com.leorsun.projecthub.model.User;
import com.leorsun.projecthub.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<Task> create(@PathVariable Long projectId, @RequestBody CreateTaskDto dto) {
        return ResponseEntity.ok(taskService.createTask(currentUser(), projectId, dto));
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<List<Task>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.listTasks(currentUser(), projectId));
    }

    @GetMapping("/api/tasks/{taskId}")
    public ResponseEntity<Task> get(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTask(currentUser(), taskId));
    }

    @PutMapping("/api/tasks/{taskId}")
    public ResponseEntity<Task> update(@PathVariable Long taskId, @RequestBody UpdateTaskDto dto) {
        return ResponseEntity.ok(taskService.updateTask(currentUser(), taskId, dto));
    }

    @DeleteMapping("/api/tasks/{taskId}")
    public ResponseEntity<?> delete(@PathVariable Long taskId) {
        taskService.deleteTask(currentUser(), taskId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/tasks/{taskId}/move")
    public ResponseEntity<Task> move(@PathVariable Long taskId, @RequestBody MoveTaskDto dto) {
        return ResponseEntity.ok(taskService.moveTask(currentUser(), taskId, dto));
    }

    @PatchMapping("/api/tasks/{taskId}/assign")
    public ResponseEntity<Task> assign(@PathVariable Long taskId, @RequestBody AssignTaskDto dto) {
        return ResponseEntity.ok(taskService.assignTask(currentUser(), taskId, dto));
    }
}

