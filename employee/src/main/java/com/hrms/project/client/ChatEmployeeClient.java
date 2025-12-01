package com.hrms.project.client;

import com.hrms.project.configuration.ChatFeignConfig;
import com.hrms.project.dto.ChatEmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "chat-service",configuration = ChatFeignConfig.class)
public interface ChatEmployeeClient {

    @PostMapping("/api/chat/employee/add")
    void addEmployee(@RequestBody ChatEmployeeDTO dto);

    @PutMapping("/api/chat/employee/update/{employeeId}")
    void updateEmployee(@PathVariable("employeeId") String employeeId,
                        @RequestBody ChatEmployeeDTO dto);

    @DeleteMapping("/api/chat/employee/delete/{employeeId}")
    void deleteEmployee(@PathVariable("employeeId") String employeeId);


}




