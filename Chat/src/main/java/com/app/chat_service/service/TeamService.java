package com.app.chat_service.service;

import java.lang.System.Logger;
import org.springframework.cache.annotation.Caching; 
import org.springframework.cache.annotation.CacheEvict;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.app.chat_service.dto.GroupChatDetailsResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.feignclient.CustomFeignContext;
import com.app.chat_service.feignclient.EmployeeClient;
import com.app.chat_service.handler.IlleagalArgumentsException;
import com.app.chat_service.handler.NotFoundException;
import com.app.chat_service.repo.ChatMessageRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TeamService {

    private final EmployeeClient employeeClient;
    private final CustomFeignContext customFeignContext;
    private ChatMessageRepository chatMessageRepository;

    public TeamService(EmployeeClient employeeClient, CustomFeignContext customFeignContext, ChatMessageRepository chatMessageRepository) {
        this.employeeClient = employeeClient;
        this.customFeignContext = customFeignContext;
        this.chatMessageRepository=chatMessageRepository;
    }

    
    

    /** Check if a given team exists */
//    @Cacheable(value = "teamExists", key = "#teamId")
 // src/main/java/com/app/chat_service/service/TeamService.java
    public boolean existsByTeamId(String teamId) {
    	
    	if(teamId==null || teamId.isBlank()) {
    		throw new IlleagalArgumentsException("TeamId cannot be null or Blank");
    	}
    	
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            // Team Service Call Success
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty();
        } catch (Exception e) {
            log.error("Error while fetching team details for {}: {}", teamId, e.getMessage());
            // Feign/Network/External API error vaste, default ga false return cheyandi, 
            // leda specific business exception ni throw cheyandi.
            // Padi poyina RuntimeException ni ippudu throw cheyadam ledhu.
            return false; 
            // NOTE: Ee implementation 'Group not found' scenario ni 'Feilure to fetch group' scenario nundi separate cheyadaniki help chestundi.
        }
    }

    /** Get all teams where an employee belongs */
    
//    @Cacheable(value = "employeeTeams", key = "#employeeId", unless="#result == null or #result.isEmpty()")
    public List<TeamResponse> getTeamsByEmployeeId(String employeeId) {
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamAllEmployees(employeeId);
            log.info("got teams {}",response);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            // Idhi important, error vasthe log cheyali
            log.error("Failed to fetch teams for employee {}: {}", employeeId, e.getMessage());
            return Collections.emptyList();
        }
    }


    /** Get team details by teamId */
//    @Cacheable(value = "teamDetails", key = "#teamId")
    public TeamResponse getGroupById(String teamId) {
    	
    	if(teamId==null || teamId.isBlank()) {
    		throw new IlleagalArgumentsException("TeamId cannot be null or empty");
    	}
    	
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                return response.getBody().get(0);
            }
        } catch (Exception e) {
        	log.error("Error fetching group by ID {}: {}", teamId, e.getMessage());
        	throw new NotFoundException("Team not found with Id: "+teamId);
        }
        throw new NotFoundException("Team not found with id: " + teamId);
    }
        	

    /** Get all members of a group */
//    @Cacheable(value = "groupMembers", key = "#teamId")
    public List<TeamResponse> getGroupMembers(String teamId) {
    	
    	if(teamId==null || teamId.isBlank()) {
    		throw new IlleagalArgumentsException("Team id cannot be null or blanck");
    	}
    	
        try {
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamById(teamId);
            log.info("fetched emplyeess by Team id {}",response);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
        	log.info("exception occured while fetching the members of {}",teamId);
            throw new NotFoundException("Team not found with Id: "+teamId);
        }
        throw new NotFoundException("Team not found with Id: "+teamId);

    }

    /** Get just the employee IDs of a given team */
//    @Cacheable(value = "teamMembers", key = "#teamId")
    public List<String> getEmployeeIdsByTeamId(String teamId) {
        List<TeamResponse> teams = getGroupMembers(teamId);
        if (teams.isEmpty()) {
            return Collections.emptyList();
        }
        return teams.get(0).getEmployees()
                .stream()
                .map(emp -> emp.getEmployeeId())
                .collect(Collectors.toList());
    }
    
    
    public ResponseEntity<List<TeamResponse>> ByEmpId(String employeeId) {
        try {
            // Call Feign client directly
            ResponseEntity<List<TeamResponse>> response = employeeClient.getTeamAllEmployees(employeeId);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody() != null ? response.getBody() : Collections.emptyList());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyList());
            }
        } catch (Exception e) {
            throw new NotFoundException("employee not found with id :"+employeeId);
        }
    }
    
//    Method for deleting the data in Redis Cache
    
    @Caching(evict = {
            @CacheEvict(value = "teamExists", key = "#teamId"),
            @CacheEvict(value = "teamDetails", key = "#teamId"),
            @CacheEvict(value = "groupMembers", key = "#teamId"),
            @CacheEvict(value = "teamMembers", key = "#teamId")
        })
        public void evictTeamCaches(String teamId) {
            log.info("Evicting all caches for teamId: {}", teamId);
        }

    
//  Method for cleaning the teams of an employee belongs
    
    @CacheEvict(value = "employeeTeams", key = "#employeeId")
	public void evictEmployeeTeamsCache(String employeeId) {
		log.info("evicting the old teams of an employee from cache of : {}", employeeId);
	}

}