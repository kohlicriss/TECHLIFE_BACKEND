package com.hrms.project.controller;

import com.hrms.project.dto.*;
import com.hrms.project.security.CheckEmployeeAccess;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.TaskServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/employee")

public class TaskController {

    @Autowired
    private TaskServiceImpl taskService;

    @PostMapping("/{tlId}/{employeeId}/{projectId}/task")
    @CheckPermission(
            value = "MY_TASKS_MY_TASK_DASHBOARD_CREATE_TASK",
            MatchParmName = "tlId",
            MatchParmFromUrl = "tlId",
            MatchParmForRoles = {"ROLE_TEAM_LEAD"}
    )
    public ResponseEntity<String> createAssignment( @PathVariable String employeeId,
                                                    @PathVariable String tlId,
                                                    @PathVariable String projectId,
                                                    @RequestPart(value = "attachedFileLinks",required = false)
                                                    MultipartFile[] attachedFileLinks,
                                                    @RequestPart TaskDTO taskDTO) throws IOException {
        return new ResponseEntity<>(taskService.createAssignment(employeeId,tlId,attachedFileLinks,projectId,taskDTO), HttpStatus.CREATED);

    }

    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/all/tasks/{employeeId}")
    @CheckPermission(
            value = "MY_TASKS_MY_TASK_DASHBOARD_GET_TASK",
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_EMPLOYEE"}
    )
    public ResponseEntity<PaginatedResponseDTO> getALlTasks(@PathVariable Integer pageNumber,
                                                            @PathVariable Integer pageSize,
                                                            @PathVariable String sortBy,
                                                            @PathVariable String sortOrder,
                                                            @PathVariable String employeeId) {
        return new ResponseEntity<>(taskService.getAllTasks(pageNumber,pageSize,sortBy,sortOrder,employeeId),HttpStatus.OK);
    }

    @GetMapping("/task/{projectId}/{taskId}")
    @CheckPermission("VIEW_TASKS")
    public ResponseEntity<TaskDTO> getTask(@PathVariable String projectId,@PathVariable String taskId){
        return new ResponseEntity<>(taskService.getTask(projectId,taskId),HttpStatus.OK);
    }


    @PutMapping("{tlId}/{employeeId}/{projectId}/task")
    @CheckPermission(
            value = "MY_TASKS_MY_TASK_DASHBOARD_EDIT_TASK",
            MatchParmName = "tlId",
            MatchParmFromUrl = "tlId",
            MatchParmForRoles = {"ROLE_TEAM_LEAD"}
    )
   public ResponseEntity<String> updateTask(@RequestPart TaskDTO taskDTO,
                                             @PathVariable String tlId,
                                             @RequestPart(value = "attachedFileLinks",required = false) MultipartFile[] attachedFileLinks,
                                             @PathVariable String employeeId,
                                             @PathVariable String projectId) throws IOException {
        return new ResponseEntity<>(taskService.updateTask(taskDTO,tlId,employeeId,attachedFileLinks,projectId),HttpStatus.CREATED);
    }

    @DeleteMapping("{projectId}/{taskId}/delete/task")
    @CheckPermission(
            value = "MY_TASKS_MY_TASK_DASHBOARD_DELETE_TASK"
    )
    public ResponseEntity<String> deleteTask(@PathVariable String projectId,@PathVariable String taskId){
        return new ResponseEntity<>(taskService.deleteTask(projectId,taskId),HttpStatus.OK);
    }

    @PostMapping("history/{projectId}/{taskId}")
    @CheckPermission("MY_TASKS_MY_TASK_DASHBOARD_TASK_CREATE_HISTORY")
    public ResponseEntity<String> createTaskHistory(@PathVariable String projectId,
                                                    @PathVariable String taskId,
                                                    @RequestPart MultipartFile[] relatedFileLinks,
                                                    @RequestPart TaskUpdateDTO taskUpdateDTO) throws IOException {
        return new ResponseEntity<>(taskService.createStatus(taskId,projectId,relatedFileLinks, taskUpdateDTO), HttpStatus.CREATED);
    }

    @PutMapping("{reviewedById}/status/{projectId}/{taskId}")
    @CheckPermission(
            value = "MY_TASKS_MY_TASK_DASHBOARD_TASK_EDIT_HISTORY"
    )
    //@CheckEmployeeAccess(param = "reviewedById", roles = {"ADMIN", "HR","MANAGER","TEAM_LEAD"})
    public ResponseEntity<String> updateTaskHistory(@PathVariable String taskId,
                                                    @PathVariable String reviewedById,
                                                    @PathVariable String projectId,
                                                    @RequestBody TaskUpdateDTO taskUpdateDTO) throws IOException {
        return new ResponseEntity<>(taskService.updateStatus(reviewedById,taskId,projectId, taskUpdateDTO),
                HttpStatus.OK);
    }

    @GetMapping("/{projectId}/{taskId}/updatetasks")
    @CheckPermission("MY_TASKS_MY_TASK_DASHBOARD_TASK_GET_TASK_HISTORY")
    //@PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR', 'MANAGER','TEAM_LEAD')")
    public ResponseEntity<List<TaskUpdateDTO>> getUpdateTasks(@PathVariable String projectId,
                                                              @PathVariable String taskId){
        return new ResponseEntity<>(taskService.getUpdateTasks(projectId,taskId),HttpStatus.OK);

    }
    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/{tlId}")
    @CheckPermission("MY_TASKS_MY_TASK_DASHBOARD_TEAM_LEAD_SIDEBAR")
    //@PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR', 'MANAGER','TEAM_LEAD')")
    public ResponseEntity<PaginatedResponseDTO> getTasks(@PathVariable Integer pageNumber,
                                                  @PathVariable Integer pageSize,
                                                  @PathVariable String sortBy,
                                                  @PathVariable String sortOrder,
                                                  @PathVariable String tlId){
        return new ResponseEntity<>(taskService.getTasks(pageNumber,pageSize,sortBy,sortOrder,tlId),HttpStatus.OK);

    }
    @DeleteMapping("/{projectId}/{taskId}/{historyId}/delete")
    @CheckPermission(
            value = "MY_TASKS_MY_TASK_DASHBOARD_TASK_DELETE_TASK_HISTORY")
    public ResponseEntity<String> deleteTaskHistory(@PathVariable String projectId,
                                                    @PathVariable String taskId,
                                                    @PathVariable Long historyId){
        return new ResponseEntity<>(taskService.deleteTaskHistory(projectId,taskId,historyId),HttpStatus.OK);
    }

    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<ProjectTasksDTO>getProjectTask(@PathVariable String projectId){
        return new ResponseEntity<>(taskService.getProjectTask(projectId),HttpStatus.OK);
    }

    @GetMapping("/project/{projectId}/employee/{employeeId}/details")
    public EmployeeTaskDetailsDTO getTaskDetails(
            @PathVariable String projectId,
            @PathVariable String employeeId) {
        return taskService.getTaskDetails(projectId, employeeId);
    }
    @GetMapping("/{projectId}/{taskId}/progress")
    public ResponseEntity<List<TaskProgressDTO>> getTaskProgress(@PathVariable String projectId,
                                                                 @PathVariable String taskId) {
        return ResponseEntity.ok(taskService.getTaskProgress(projectId, taskId));
    }

}