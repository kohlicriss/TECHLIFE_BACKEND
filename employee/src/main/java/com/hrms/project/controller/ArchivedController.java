package com.hrms.project.controller;

import com.hrms.project.dto.ArchivedDTo;
import com.hrms.project.dto.PaginatedDTO;
import com.hrms.project.entity.Archive;
import com.hrms.project.security.CheckPermission;
import com.hrms.project.service.ArchivedServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/employee")
public class ArchivedController {

    @Autowired
    private ArchivedServiceImpl archivedService;

    @GetMapping("{pageNumber}/{pageSize}/{sortBy}/{sortOrder}/terminated/employees")
    @CheckPermission(value="EMPLOYEES_TERMINATE" ,
            MatchParmName = "employeeId",
            MatchParmFromUrl = "employeeId",
            MatchParmForRoles = {"ROLE_ADMIN"}
    )


    public CompletableFuture<ResponseEntity<PaginatedDTO<ArchivedDTo>>> getTerminatedEmployees(@PathVariable Integer pageNumber,
                                                                                               @PathVariable Integer pageSize,
                                                                                               @PathVariable String sortBy,
                                                                                               @PathVariable String sortOrder) {
        return archivedService.getEmployee(pageNumber,pageSize,sortBy,sortOrder).thenApply(terminated->ResponseEntity.status(HttpStatus.OK).body(terminated));

    }



}
