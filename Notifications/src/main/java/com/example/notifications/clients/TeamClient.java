package com.example.notifications.clients;



import com.example.notifications.dtos.TeamResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "employee-service", contextId = "teamClient")
public interface TeamClient {
    @GetMapping("/api/employee/team/{teamId}")
    TeamResponse getEmployeesInTeam(@PathVariable("teamId") String teamId);

}

