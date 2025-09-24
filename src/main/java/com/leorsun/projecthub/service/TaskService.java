package com.leorsun.projecthub.service;

import com.leorsun.projecthub.dto.AssignTaskDto;
import com.leorsun.projecthub.dto.CreateTaskDto;
import com.leorsun.projecthub.dto.MoveTaskDto;
import com.leorsun.projecthub.dto.UpdateTaskDto;
import com.leorsun.projecthub.model.*;
import com.leorsun.projecthub.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       ProjectMemberRepository memberRepository,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    private void assertMember(User user, Long projectId) {
        if (!memberRepository.existsByProject_IdAndUser_Id(projectId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a project member");
        }
    }

    private boolean isAtLeast(User user, Long projectId, ProjectRole role) {
        return memberRepository.findByProject_IdAndUser_Id(projectId, user.getId())
                .map(m -> m.getRole().atLeast(role)).orElse(false);
    }

    public Task createTask(User actor, Long projectId, CreateTaskDto dto) {
        assertMember(actor, projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        Task task = new Task();
        task.setProject(project);
        task.setTitle(nonBlank(dto.getTitle(), "Title is required"));
        task.setDescription(dto.getDescription());
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());
        task.setDueDate(dto.getDueDate());
        task.setReporter(actor);
        if (dto.getAssigneeId() != null) {
            User assignee = userRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee not found"));
            task.setAssignee(assignee);
        }
        // set orderIndex to end of current status list
        List<Task> column = taskRepository.findByProject_IdAndStatusOrderByOrderIndexAsc(projectId, task.getStatus());
        int nextIndex = column.stream().map(Task::getOrderIndex).filter(i -> i != null).max(Comparator.naturalOrder()).orElse(-1) + 1;
        task.setOrderIndex(nextIndex);
        return taskRepository.save(task);
    }

    public List<Task> listTasks(User user, Long projectId) {
        assertMember(user, projectId);
        return taskRepository.findByProject_IdOrderByOrderIndexAsc(projectId);
    }

    public Task getTask(User user, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        assertMember(user, task.getProject().getId());
        return task;
    }

    public Task updateTask(User actor, Long taskId, UpdateTaskDto dto) {
        Task task = getTask(actor, taskId);
        Long projectId = task.getProject().getId();
        // Allow MEMBER to edit tasks; admins/owners naturally allowed
        if (!isAtLeast(actor, projectId, ProjectRole.MEMBER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
        }
        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getStatus() != null && dto.getStatus() != task.getStatus()) {
            task.setStatus(dto.getStatus());
            // reset order to end of new column
            List<Task> column = taskRepository.findByProject_IdAndStatusOrderByOrderIndexAsc(projectId, task.getStatus());
            int nextIndex = column.stream().map(Task::getOrderIndex).filter(i -> i != null).max(Comparator.naturalOrder()).orElse(-1) + 1;
            task.setOrderIndex(nextIndex);
        }
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());
        task.setDueDate(dto.getDueDate());
        if (dto.getAssigneeId() != null) {
            User assignee = userRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee not found"));
            task.setAssignee(assignee);
        } else if (dto.getAssigneeId() == null) {
            // explicit null means unassign if present in payload
        }
        return taskRepository.save(task);
    }

    public void deleteTask(User actor, Long taskId) {
        Task task = getTask(actor, taskId);
        Long projectId = task.getProject().getId();
        boolean canDelete = isAtLeast(actor, projectId, ProjectRole.ADMIN) || (task.getReporter() != null && task.getReporter().getId().equals(actor.getId()));
        if (!canDelete) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role to delete task");
        }
        taskRepository.delete(task);
    }

    public Task moveTask(User actor, Long taskId, MoveTaskDto dto) {
        Task task = getTask(actor, taskId);
        if (!isAtLeast(actor, task.getProject().getId(), ProjectRole.MEMBER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
        }
        if (dto.getStatus() != null && dto.getStatus() != task.getStatus()) {
            task.setStatus(dto.getStatus());
            // place at end of new column
            List<Task> column = taskRepository.findByProject_IdAndStatusOrderByOrderIndexAsc(task.getProject().getId(), task.getStatus());
            int nextIndex = column.stream().map(Task::getOrderIndex).filter(i -> i != null).max(Comparator.naturalOrder()).orElse(-1) + 1;
            task.setOrderIndex(nextIndex);
        }
        if (dto.getOrderIndex() != null) {
            task.setOrderIndex(dto.getOrderIndex());
        }
        return taskRepository.save(task);
    }

    public Task assignTask(User actor, Long taskId, AssignTaskDto dto) {
        Task task = getTask(actor, taskId);
        if (!isAtLeast(actor, task.getProject().getId(), ProjectRole.MEMBER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
        }
        if (dto.getAssigneeId() == null) {
            task.setAssignee(null);
        } else {
            User assignee = userRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee not found"));
            task.setAssignee(assignee);
        }
        return taskRepository.save(task);
    }

    private String nonBlank(String s, String message) {
        if (s == null || s.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return s;
    }
}

