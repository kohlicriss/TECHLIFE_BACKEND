package com.hrms.project.service;

import com.hrms.project.client.NotificationClient;
import com.hrms.project.configuration.TaskId;
import com.hrms.project.configuration.TaskUpdateId;
import com.hrms.project.dto.*;
import com.hrms.project.entity.*;
import com.hrms.project.handlers.*;
import com.hrms.project.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TaskServiceImpl {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TaskUpdateRepository taskUpdateRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private ProjectService projectService;

    public String createAssignment(String employeeId, String tlId,
                                   MultipartFile[] attachedFileLinks,
                                   String projectId, TaskDTO taskDTO) throws IOException {
        try {
            log.info("Creating assignment for employeeId: {}, projectId: {}, taskTitle: {}", employeeId, projectId, taskDTO.getTitle());

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id " + employeeId));

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ProjectNotFoundException("Project not found with id " + projectId));

            boolean exists = taskRepository.existsByTitleAndProject_ProjectIdAndEmployee_EmployeeId(
                    taskDTO.getTitle(), projectId, employeeId
            );

            if (exists) {
                log.warn("Duplicate task found for employeeId: {}, projectId: {}, taskTitle: {}", employeeId, projectId, taskDTO.getTitle());
                throw new DuplicateResourceException("Task already exists for this employee in this project");
            }

            long count = taskRepository.count();
            String newTaskId = "TASK" + String.format("%03d", count + 1);
            while (taskRepository.findById(new TaskId(newTaskId, projectId)).isPresent()) {
                count++;
                newTaskId = "TASK" + String.format("%03d", count + 1);
            }

            TaskId taskId = new TaskId(newTaskId, projectId);
            Task task = modelMapper.map(taskDTO, Task.class);
            task.setCreatedBy(tlId);
            task.setId(taskId);
            task.setEmployee(employee);
            task.setProject(project);

            if (attachedFileLinks != null && attachedFileLinks.length > 0 && !attachedFileLinks[0].isEmpty()) {
                log.info("Uploading {} attachment(s) for employeeId: {}", attachedFileLinks.length, employeeId);
                List<String> s3Keys = s3Service.uploadMultipleFiles(employeeId, "attachedFileLinks", attachedFileLinks);
                task.setAttachedFileLinks(s3Keys);
            }

            taskRepository.save(task);
            log.info("Task '{}' (ID: {}) created successfully for employeeId: {}", task.getTitle(), taskId.getTaskId(), employeeId);
            projectService.refreshTaskCounts(project);
            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(employeeId)
                    .message("A new task '" + task.getTitle() + "' has been assigned to you by " + tlId)
                    .sender(tlId)
                    .type("TASK")
                    .link("/tasks/" + employeeId)
                    .category("TASK")
                    .kind("CREATED")
                    .subject("New Task Assigned")
                    .build();

            notificationClient.send(notification);
            log.info("Notification sent to employeeId: {}", employeeId);

            return "Assignment Created Successfully";
        } catch (EmployeeNotFoundException | ProjectNotFoundException | DuplicateResourceException ex) {
            log.error("Error creating task for employee {} in project {}: {}", employeeId, projectId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error creating task for employee {} in project {}: {}", employeeId, projectId, ex.getMessage(), ex);
            throw new RuntimeException("Unexpected error creating assignment");
        }
    }

    public PaginatedResponseDTO getAllTasks(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String employeeId) {
        try {
            log.info("Fetching tasks for employeeId: {}", employeeId);
            Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));

            Page<Task> tasksPage = taskRepository.findByEmployee_EmployeeId(employeeId, pageable);

            if (tasksPage.isEmpty()) {
                log.warn("No tasks assigned to employeeId: {}", employeeId);
                throw new TaskNotFoundException("This employee has no tasks assigned");
            }

            List<AllTaskDTO> taskDTOs = tasksPage.getContent().stream()
                    .map(task -> {
                        AllTaskDTO dto = new AllTaskDTO();
                        dto.setId(task.getId().getTaskId());
                        dto.setStatus(task.getStatus());
                        dto.setTitle(task.getTitle());
                        dto.setPriority(task.getPriority());
                        dto.setStartDate(task.getCreatedDate());
                        dto.setDueDate(task.getDueDate());
                        dto.setProjectId(task.getProject().getProjectId());
                        dto.setAssignedTo(task.getAssignedTo());
                        dto.setCreatedBy(task.getCreatedBy());
                        return dto;
                    }).collect(Collectors.toList());

            log.info("Returning {} tasks for employeeId {}", taskDTOs.size(), employeeId);

            PaginatedResponseDTO response = new PaginatedResponseDTO();
            response.setContent(taskDTOs);
            response.setPageNumber(tasksPage.getNumber());
            response.setPageSize(tasksPage.getSize());
            response.setTotalElements(tasksPage.getTotalElements());
            response.setTotalPages(tasksPage.getTotalPages());
            response.setLast(tasksPage.isLast());
            response.setFirst(tasksPage.isFirst());
            response.setNumberOfElements(tasksPage.getNumberOfElements());

            return response;
        } catch (EmployeeNotFoundException | TaskNotFoundException ex) {
            log.error("Error fetching tasks for employee {}: {}", employeeId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error fetching tasks for employee {}: {}", employeeId, ex.getMessage(), ex);
            throw new RuntimeException("Error fetching tasks");
        }
    }

    public String updateTask(TaskDTO taskDTO, String tlId, String employeeId,
                             MultipartFile[] attachedFileLinks, String projectId) throws IOException {
        try {
            log.info("Updating task '{}' for employeeId: {}, projectId: {}", taskDTO.getId(), employeeId, projectId);

            TaskId compositeTaskId = new TaskId(taskDTO.getId(), projectId);

            Task task = taskRepository.findById(compositeTaskId)
                    .orElseThrow(() -> new TaskNotFoundException("Task not found"));
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

            // ✅ Preserve existing attached files if no new ones uploaded
            List<String> existingFiles = task.getAttachedFileLinks();

            if (attachedFileLinks != null && attachedFileLinks.length > 0 && !attachedFileLinks[0].isEmpty()) {
                log.info("Uploading {} new file(s) for task {}", attachedFileLinks.length, taskDTO.getId());

                // ✅ Delete old files before uploading new ones
                if (existingFiles != null && !existingFiles.isEmpty()) {
                    log.debug("Deleting old {} attached file(s) for task {}", existingFiles.size(), taskDTO.getId());
                    for (String key : existingFiles) {
                        s3Service.deleteFile(key);
                    }
                }

                // ✅ Upload new files
                List<String> s3Keys = s3Service.uploadMultipleFiles(
                        task.getEmployee().getEmployeeId(),
                        "attachedFileLinks",
                        attachedFileLinks
                );
                taskDTO.setAttachedFileLinks(s3Keys);
                log.debug("Uploaded new S3 keys: {}", s3Keys);
            } else {
                // ✅ No new files: keep existing file links
                taskDTO.setAttachedFileLinks(existingFiles);
                log.info("No new files uploaded — keeping existing attached files.");
            }

            // ✅ Map updated fields
            taskDTO.setProjectId(projectId);
            modelMapper.map(taskDTO, task);
            task.setCreatedBy(tlId);
            task.setId(compositeTaskId);
            task.setEmployee(employee);
            task.setProject(project);

            taskRepository.save(task);
            projectService.refreshTaskCounts(project);

            log.info("Task '{}' updated successfully", task.getTitle());

            // ✅ Notify assigned employee
            notificationClient.send(NotificationRequest.builder()
                    .receiver(employeeId)
                    .message("Your task '" + task.getTitle() + "' was updated.")
                    .sender(tlId)
                    .type("TASK")
                    .link("/tasks/" + employeeId)
                    .category("TASK")
                    .kind("UPDATED")
                    .subject("Task Updated")
                    .build());

            return "Assignment Updated Successfully";
        } catch (Exception ex) {
            log.error("Error updating task '{}' for employee {} in project {}: {}",
                    taskDTO.getId(), employeeId, projectId, ex.getMessage(), ex);
            throw ex;
        }
    }


    // ------------------------- CREATE STATUS -------------------------
    public String createStatus(String taskId, String projectId,
                               MultipartFile[] relatedFileLinks, TaskUpdateDTO taskUpdateDTO) throws IOException {
        try {
            log.info("Creating status update for taskId: {}, projectId: {}", taskId, projectId);
            TaskId taskKey = new TaskId(taskId, projectId);
            Task task = taskRepository.findById(taskKey)
                    .orElseThrow(() -> new TaskNotFoundException("Task not found"));

            List<TaskUpdate> existingUpdates = taskUpdateRepository
                    .findByTask_Id_TaskIdAndTask_Id_ProjectIdAndChangesAndNote(
                            taskId, projectId, taskUpdateDTO.getChanges(), taskUpdateDTO.getNote()
                    );

            boolean duplicateExists = existingUpdates.stream().anyMatch(t ->
                    Objects.equals(t.getRelatedLinks(), taskUpdateDTO.getRelatedLinks())
            );
            if (duplicateExists) {
                log.warn("Duplicate task update detected for taskId {} in project {}", taskId, projectId);
                return "Duplicate task update already exists. Skipping save.";
            }

            Long maxUpdateNumber = taskUpdateRepository.findMaxUpdateNumberByTask(taskId, projectId);
            Long newUpdateNumber = (maxUpdateNumber == null) ? 1L : maxUpdateNumber + 1;
            TaskUpdateId compositeUpdateId = new TaskUpdateId(taskKey, newUpdateNumber);

            TaskUpdate taskUpdate = new TaskUpdate();
            taskUpdate.setTask(task);
            taskUpdate.setId(compositeUpdateId);
            taskUpdate.setUpdatedDate(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            taskUpdate.setChanges(taskUpdateDTO.getChanges());
            taskUpdate.setNote(taskUpdateDTO.getNote());
            taskUpdate.setRelatedLinks(taskUpdateDTO.getRelatedLinks());
            taskUpdate.setRemark(taskUpdateDTO.getRemark());

            if (relatedFileLinks != null && relatedFileLinks.length > 0 && !relatedFileLinks[0].isEmpty()) {
                log.info("Uploading {} related file(s) for status update {}", relatedFileLinks.length, newUpdateNumber);
                List<String> s3Keys = s3Service.uploadMultipleFiles(
                        task.getEmployee().getEmployeeId(),
                        "relatedFileLinks",
                        relatedFileLinks
                );
                taskUpdate.setRelatedFileLinks(s3Keys);
                log.debug("Uploaded related file S3 keys: {}", s3Keys);
            }

            taskUpdateRepository.save(taskUpdate);
            log.info("Status update {} created successfully for task {}", newUpdateNumber, taskId);

            notificationClient.send(NotificationRequest.builder()
                    .receiver(task.getEmployee().getEmployeeId())
                    .message("A new status update was added for task '" + task.getTitle() + "'.")
                    .sender("SYSTEM")
                    .type("TASK")
                    .link("/tasks/"+task.getEmployee().getEmployeeId()+"/taskview/" + projectId + "/"+taskId )
                    .category("TASK")
                    .kind("STATUS_CREATED")
                    .subject("New Task Status Update")
                    .build());
            return "Task update saved successfully";
        } catch (Exception ex) {
            log.error("Error creating status for task {} in project {}: {}", taskId, projectId, ex.getMessage(), ex);
            throw ex;
        }
    }

    // ------------------------- UPDATE STATUS -------------------------
    public String updateStatus(String reviewedById, String taskId,
                               String projectId, TaskUpdateDTO taskUpdateDTO) throws IOException {
        try {
            log.info("Updating status {} for taskId: {}, projectId: {}", taskUpdateDTO.getId(), taskId, projectId);

            TaskId taskKey = new TaskId(taskId, projectId);
            TaskUpdateId compositeUpdateId = new TaskUpdateId(taskKey, taskUpdateDTO.getId());

            Task task = taskRepository.findById(taskKey)
                    .orElseThrow(() -> new TaskNotFoundException("Task not found"));
            TaskUpdate taskUpdate = taskUpdateRepository.findById(compositeUpdateId)
                    .orElseThrow(() -> new TaskNotFoundException("Update not found"));

            taskUpdate.setUpdatedDate(taskUpdateDTO.getUpdatedDate());
            taskUpdate.setNote(taskUpdateDTO.getNote());
            taskUpdate.setChanges(taskUpdateDTO.getChanges());
            taskUpdate.setRelatedLinks(taskUpdateDTO.getRelatedLinks());
            //  taskUpdate.setRelatedFileLinks(taskUpdateDTO.getRelatedFileLinks());
            taskUpdate.setReviewedBy(reviewedById);
            taskUpdate.setRemark(taskUpdateDTO.getRemark());


            taskUpdate.setTask(task);
            taskUpdateRepository.save(taskUpdate);
            log.info("Status {} updated successfully for task {}", taskUpdateDTO.getId(), taskId);

            notificationClient.send(NotificationRequest.builder()
                    .receiver(task.getEmployee().getEmployeeId())
                    .message("Status updated for task '" + task.getTitle() + "'. Remark: " + taskUpdateDTO.getRemark())
                    .sender(reviewedById)
                    .type("TASK")
                    .link("/tasks/"+task.getEmployee().getEmployeeId()+"/taskview/" + projectId +"/"+ taskId )
                    .category("TASK")
                    .kind("STATUS_UPDATED")
                    .subject("Task Status Updated")
                    .build());
            return "Updated Successfully";
        } catch (Exception ex) {
            log.error("Error updating status {} for task {}: {}", taskUpdateDTO.getId(), taskId, ex.getMessage(), ex);
            throw ex;
        }
    }

    public TaskDTO getTask(String projectId, String taskId) {
        log.info("Fetching task details for taskId: {}, projectId: {}", taskId, projectId);

        TaskId taskKey = new TaskId(taskId, projectId);

        Task task = taskRepository.findById(taskKey)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));

        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId().getTaskId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setCreatedBy(task.getCreatedBy());
        dto.setAssignedTo(task.getAssignedTo());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setCreatedDate(task.getCreatedDate());
        dto.setCompletedDate(task.getCompletedDate());
        dto.setDueDate(task.getDueDate());
        dto.setRating(task.getRating());
        dto.setRemark(task.getRemark());
        dto.setCompletionNote(task.getCompletionNote());
        dto.setRelatedLinks(task.getRelatedLinks());
        List<String> presignedUrls = Optional.ofNullable(task.getAttachedFileLinks())
                .orElse(Collections.emptyList())
                .stream()
                .map(s3Service::generatePresignedUrl)
                .toList();


        dto.setAttachedFileLinks(presignedUrls);

        return dto;
    }

    public List<TaskUpdateDTO> getUpdateTasks(String projectId, String taskId) {
        log.info("Fetching task updates for projectId={} and taskId={}", projectId, taskId);

        List<TaskUpdateDTO> updates = taskUpdateRepository.findAll()
                .stream()
                .filter(taskUpdate ->
                        taskUpdate.getTask().getId().getTaskId().equals(taskId) &&
                                taskUpdate.getTask().getId().getProjectId().equals(projectId)
                )
                .map(update -> {
                    TaskUpdateDTO dto = new TaskUpdateDTO();
                    dto.setId(update.getId().getUpdateNumber());
                    dto.setChanges(update.getChanges());
                    dto.setNote(update.getNote());
                    dto.setRelatedLinks(update.getRelatedLinks());
                    dto.setUpdatedDate(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                    dto.setReviewedBy(update.getReviewedBy());
                    dto.setRemark(update.getRemark());

                    List<String> presignedUrls = update.getRelatedFileLinks().stream()
                            .map(s3Service::generatePresignedUrl)
                            .toList();
                    dto.setRelatedFileLinks(presignedUrls);

                    log.info("Mapped task update {} for taskId={}", update.getId().getUpdateNumber(), taskId);
                    return dto;
                })
                .toList();

        log.info("Fetched {} task updates for taskId={}", updates.size(), taskId);
        return updates;
    }

    public String deleteTask(String projectId, String taskId) {
        log.info("Deleting task with projectId={} and taskId={}", projectId, taskId);

        TaskId taskKey = new TaskId(taskId, projectId);
        Task task = taskRepository.findById(taskKey)
                .orElseThrow(() -> {
                    log.error("Task not found for projectId={} taskId={}", projectId, taskId);
                    return new TaskNotFoundException("Task not found");
                });

        if (task.getAttachedFileLinks() != null && !task.getAttachedFileLinks().isEmpty()) {
            for (String key : task.getAttachedFileLinks()) {
                try {
                    s3Service.deleteFile(key);
                    log.info("Deleted attached file from S3: {}", key);
                } catch (Exception e) {
                    log.error("Failed to delete attached file {}: {}", key, e.getMessage());
                }
            }
        }

        taskRepository.deleteById(taskKey);
        projectService.refreshTaskCounts(task.getProject());
        log.info("Task deleted from repository: {}", taskId);

        try {
            notificationClient.send(
                    NotificationRequest.builder()
                            .receiver(task.getEmployee().getEmployeeId())
                            .message("Task '" + task.getTitle() + "' has been deleted.")
                            .sender("SYSTEM")
                            .type("TASK")
                            .link("/tasks/" + task.getEmployee().getEmployeeId())
                            .category("TASK")
                            .kind("DELETED")
                            .subject("Task Deleted")
                            .build()
            );
            log.info("Notification sent for deleted task {}", taskId);
        } catch (Exception e) {
            log.error("Failed to send notification for deleted task {}: {}", taskId, e.getMessage());
        }

        return "Task deleted successfully";
    }

    public PaginatedResponseDTO getTasks(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String tlId) {
        log.info("Fetching tasks created by TL {} with pageNumber={} pageSize={}", tlId, pageNumber, pageSize);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Task> tasksPage = taskRepository.findByCreatedBy(tlId, pageable);

        List<TaskDTO> taskDTOs = tasksPage.getContent().stream()
                .map(task -> {
                    TaskDTO dto = new TaskDTO();
                    dto.setId(task.getId().getTaskId());
                    dto.setTitle(task.getTitle());
                    dto.setDescription(task.getDescription());
                    dto.setCreatedBy(task.getCreatedBy());
                    dto.setAssignedTo(task.getAssignedTo());
                    dto.setStatus(task.getStatus());
                    dto.setPriority(task.getPriority());
                    dto.setCreatedDate(task.getCreatedDate());
                    dto.setCompletedDate(task.getCompletedDate());
                    dto.setDueDate(task.getDueDate());
                    dto.setRating(task.getRating());
                    dto.setRemark(task.getRemark());
                    dto.setCompletionNote(task.getCompletionNote());
                    dto.setRelatedLinks(task.getRelatedLinks());
                    dto.setAttachedFileLinks(task.getAttachedFileLinks());
                    dto.setProjectId(task.getId().getProjectId());

                    log.info("Mapped task {} to DTO", task.getId().getTaskId());
                    return dto;
                })
                .toList();

        PaginatedResponseDTO response = new PaginatedResponseDTO();
        response.setTasks(taskDTOs);
        response.setPageNumber(tasksPage.getNumber());
        response.setPageSize(tasksPage.getSize());
        response.setTotalElements(tasksPage.getTotalElements());
        response.setTotalPages(tasksPage.getTotalPages());
        response.setLast(tasksPage.isLast());
        response.setFirst(tasksPage.isFirst());
        response.setNumberOfElements(tasksPage.getNumberOfElements());

        log.info("Fetched {} tasks for TL {}", taskDTOs.size(), tlId);
        return response;
    }

    public String deleteTaskHistory(String projectId, String taskId, Long id) {
        log.info("Deleting task history for taskId={} projectId={} updateId={}", taskId, projectId, id);

        TaskId taskKey = new TaskId(taskId, projectId);
        TaskUpdateId taskUpdateId = new TaskUpdateId(taskKey, id);
        TaskUpdate task = taskUpdateRepository.findById(taskUpdateId)
                .orElseThrow(() -> {
                    log.error("Task update not found for taskId={} updateId={}", taskId, id);
                    return new TaskNotFoundException("Task update not found");
                });

        if (task.getRelatedFileLinks() != null && !task.getRelatedFileLinks().isEmpty()) {
            for (String key : task.getRelatedFileLinks()) {
                try {
                    s3Service.deleteFile(key);
                    log.info("Deleted related file from S3: {}", key);
                } catch (Exception e) {
                    log.error("Failed to delete related file {}: {}", key, e.getMessage());
                }
            }
        }

        taskUpdateRepository.deleteById(taskUpdateId);
        log.info("Deleted task update {} from repository", id);

        try {
            notificationClient.send(
                    NotificationRequest.builder()
                            .receiver(task.getTask().getEmployee().getEmployeeId())
                            .message("A status update for task '" + task.getTask().getTitle() + "' has been deleted.")
                            .sender("SYSTEM")
                            .type("TASK")
                            .link("/tasks/"+task.getTask().getEmployee().getEmployeeId()+"/taskview/" + projectId +"/"+ taskId )
                            .category("TASK")
                            .kind("STATUS_DELETED")
                            .subject("Task Update Deleted")
                            .build()
            );
            log.info("Notification sent for deleted task update {}", id);
        } catch (Exception e) {
            log.error("Failed to send notification for deleted task update {}: {}", id, e.getMessage());
        }

        return "Task update deleted successfully";
    }

    public ProjectTasksDTO getProjectTask(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        List<Task> tasks = taskRepository.findByProject_ProjectId(projectId);

        List<TaskSimpleDTO> taskDTOs = tasks.stream()
                .map(task -> new TaskSimpleDTO(
                        task.getId().getTaskId(),
                        task.getTitle(),
                        task.getStatus(),
                        task.getDescription()
                ))
                .collect(Collectors.toList());

        return new ProjectTasksDTO(project.getProjectId(), project.getTitle(), taskDTOs);
    }


    public ProjectWithTasksDTO getProjectWithTaskHistory(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        List<TaskWithUpdatesDTO> taskDTOs = project.getAssignments().stream()
                .map(task -> {
                    List<TaskUpdateSimpleDTO> updates = task.getUpdateHistory().stream()
                            .map(update -> new TaskUpdateSimpleDTO(
                                    update.getId().getUpdateNumber(),
                                    update.getChanges(),
                                    update.getUpdatedDate()
                            ))
                            .sorted(Comparator.comparing(TaskUpdateSimpleDTO::getUpdateNumber))
                            .toList();

                    return new TaskWithUpdatesDTO(
                            task.getId().getTaskId(),
                            task.getTitle(),
                            task.getStatus(),
                            updates
                    );
                })
                .toList();

        return new ProjectWithTasksDTO(project.getProjectId(), project.getTitle(), taskDTOs);
    }

    public EmployeeTaskDetailsDTO getTaskDetails(String projectId, String employeeId) {
        List<Task> tasks = taskRepository.findEmployeeTasksInProject(projectId, employeeId);

        long totalTasks = tasks.size();
        long tasksDone = tasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .count();

        double percentageCompleted = totalTasks == 0 ? 0 : (tasksDone * 100.0 / totalTasks);

        return new EmployeeTaskDetailsDTO(tasksDone, totalTasks, percentageCompleted);
    }


    public List<TaskProgressDTO> getTaskProgress(String projectId, String taskId) {
        log.info("Fetching task progress for projectId={} and taskId={}", projectId, taskId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM h:mm a");

        return taskUpdateRepository.findAll()
                .stream()
                .filter(taskUpdate ->
                        taskUpdate.getTask().getId().getProjectId().equals(projectId) &&
                                taskUpdate.getTask().getId().getTaskId().equals(taskId)
                )
                .map(update -> new TaskProgressDTO(
                        update.getUpdatedDate() != null ? update.getUpdatedDate().format(formatter) : null,
                        update.getChanges(),
                        update.getNote()
                ))
                .collect(Collectors.toList());
    }

}