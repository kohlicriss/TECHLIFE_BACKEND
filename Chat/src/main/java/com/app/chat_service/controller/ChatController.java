package com.app.chat_service.controller;

import java.awt.print.Pageable;
import java.net.Authenticator;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.chat_service.dto.ChatMessageOverviewDTO;
import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.dto.EmployeeDTO;
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.feignclient.CustomFeignContext;
import com.app.chat_service.feignclient.EmployeeClient;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.employee_details;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.service.AllEmployees;
import com.app.chat_service.service.ChatForwardService;
import com.app.chat_service.service.ChatMessageOverviewService;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatService;
import com.app.chat_service.service.EmployeeDetailsService;
import com.app.chat_service.service.TeamService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final AllEmployees allEmployees;
    private final TeamService teamService;
    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final EmployeeClient employeeClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageOverviewService chatMessageOverviewService;
    private final ChatMessageRepository chatMessageRepository;
    private final EmployeeDetailsService employeeDetailsService;   
    private final ChatForwardService chatForwardService;
    private final CustomFeignContext customFeignContext;

    
    
    /** Fetch messages between employee and chatId (could be private or group) */
    @GetMapping("/{empId}/{chatId}")
    public ResponseEntity<List<ChatMessageOverviewDTO>> getChatMessages(
            @PathVariable("empId") String empId,
            @PathVariable("chatId") String chatId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestHeader String Authorization){
    	
    	customFeignContext.setToken(Authorization);
    	
    	log.info("Fetching chat messages for empId: {}, chatId: {}, page: {}, size: {}", empId, chatId, page, size);

    	org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        List<ChatMessageOverviewDTO> messages = chatMessageOverviewService.getChatMessages(empId, chatId, pageable);
        Collections.reverse(messages);
        return ResponseEntity.ok(messages);
    }

    /** Sidebar Overview (Private + Group Chats) */
    @GetMapping("/overview/{employeeId}")
    public ResponseEntity<List<Map<String, Object>>> getChatOverview(
            @PathVariable("employeeId") String employeeId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
    		@RequestHeader String Authorization) {
    	
    	log.info("header received {}" ,Authorization);
    	customFeignContext.setToken(Authorization);
    	log.info("Fetching chat overview for employeeId: {}, page: {}, size: {} ", employeeId, page, size);
        List<Map<String, Object>> chatOverview = chatMessageService.getChattedEmployeesInSameTeam(employeeId, page, size);
        return ResponseEntity.ok(chatOverview);

    }

    /** Send message (REST API) */
    @PostMapping("/send")
    public String sendChat(@RequestBody ChatMessageRequest request) {
        log.info("Received REST API request to send chat message: {}", request);

        return chatService.sendMessage(request);
    }

    /** Fetch all messages of a Team */
    @GetMapping("/team/{teamId}")
    public List<ChatMessageResponse> getTeamMessages(@PathVariable String teamId) {
        log.info("Fetching all messages for teamId: {}", teamId);

        List<ChatMessage> messages = chatService.getTeamMessages(teamId);
        return messages.stream().map(this::toResponse).toList();
    }

    /** Private chat history */
    @GetMapping("/history/private")
    public ResponseEntity<List<ChatMessageResponse>> getPrivateChatHistory(
            @RequestParam("sender") String sender,
            @RequestParam("receiver") String receiver) {
        log.info("Fetching private chat history between sender: {} and receiver: {}", sender, receiver);

        List<ChatMessage> messages = chatService.getPrivateChatHistory(sender, receiver);
        return ResponseEntity.ok(messages.stream().map(this::toResponse).toList());
    }


    /** Get Employee details */
    @GetMapping("/employee/{id}")
    public employee_details getEmployeeById(@PathVariable("id") String id) {
        log.info("Fetching employee details for id: {}", id);
        return allEmployees.getEmployeeById(id);

    }

    /** Team details */
    @GetMapping("/team/employee/{teamId}")
    public List<TeamResponse> getTeamById(@PathVariable("teamId") String teamId) {
        log.info("Fetching team details for teamId: {}", teamId);
        
    	List<TeamResponse> memberString=teamService.getGroupMembers(teamId);
    	return memberString;
    	}

    /** Teams for an Employee */
    @GetMapping("/employee/team/{employeeId}")
    public ResponseEntity<List<TeamResponse>> getTeamsByEmpId(@PathVariable("employeeId") String employeeId) {
        log.info("Fetching teams for employeeId: {}", employeeId);

        return teamService.ByEmpId(employeeId);
    }


    /** Helper to map ChatMessage -> ChatMessageResponse */
    private ChatMessageResponse toResponse(ChatMessage msg) {
        return new ChatMessageResponse(
                msg.getId(),
                msg.getSender(),
                msg.getReceiver(),
                msg.getGroupId(),
                msg.getContent(),
                msg.getFileName(),
                msg.getFileType(),
                msg.getFileSize(),   // ✅ Pass fileSize in correct position
                msg.getType(),
                msg.getTimestamp(),
                msg.getFileData(),
                msg.getClientId(),
                msg.getDuration()
                );
    }
    
    
    @PostMapping("/internal/notify-team-update")
    public ResponseEntity<Void> notifyTeamUpdate(@RequestParam String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Received team update notification for teamId: {}. Broadcasting overview.", teamId);
        // This service call will update the sidebar for all team members
        chatMessageService.broadcastGroupChatOverview(teamId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/employee/add")
    public void addEmployee(@RequestBody EmployeeDTO employeeDtoList) {
    	log.info("Adding {} employees", employeeDtoList);    	log.info("employee image {}",employeeDtoList.getEmployeeImage());
    	employeeDetailsService.addEmployee(employeeDtoList);
    }
    	

    
    @PutMapping("/employee/update/{employeeId}")
    public employee_details empupdate(
    		@PathVariable String employeeId,
    		@RequestBody EmployeeDTO updateDetails) {
    	log.info("Updating employee with id: {}", employeeId);
    	return employeeDetailsService.updateEmployee(employeeId, updateDetails);
    	
    }
    	

    
    @DeleteMapping("/employee/delete/{employeeId}")
    public ResponseEntity<String> deleteEmployee(@PathVariable String employeeId){
        log.info("Deleting employee with id: {}", employeeId);

    	employeeDetailsService.deleteById(employeeId);
    	return ResponseEntity.ok("Employee Succesfully Deleted");   	
    }
    
    
    
    
//    method for clearing the Redis Cache
    @PostMapping("/internal/cache/evict-team")
    public ResponseEntity<Void> evictTeamCache(@RequestParam String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Received request to evict cache for teamId: {}", teamId);
        teamService.evictTeamCaches(teamId);
        return ResponseEntity.ok().build();
    }
    
    
    
//    searching messages in chat
    
    @GetMapping("/search")
    public ResponseEntity<List<ChatMessageOverviewDTO>> searchChatMessages(
            @RequestParam("userId") String userId,
            @RequestParam("chatId") String chatId,
            @RequestParam("query") String query) {
        log.info("Searching for messages with query '{}' for userId '{}' in chatId '{}'", query, userId, chatId);

        List<ChatMessageOverviewDTO> results = chatMessageService.searchMessages(userId, chatId, query);
        log.info("Found {} messages matching the search query", results.size());

        return ResponseEntity.ok(results);
    }
    
    
    
//    Navigating to the searched or founded message
    @GetMapping("/context")
    public ResponseEntity<List<ChatMessageOverviewDTO>> getMessageContext(
            @RequestParam("messageId") Long messageId,
            @RequestParam("userId") String userId,
            @RequestParam("chatId") String chatId) {
    	
        log.info("Fetching message context for messageId '{}', userId '{}', chatId '{}'", messageId, userId, chatId);

        List<ChatMessageOverviewDTO> contextMessages = chatMessageOverviewService.getMessageContext(messageId, userId, chatId);
        return ResponseEntity.ok(contextMessages);
    }
    
    
//    For group Info models
    @GetMapping("/{chatId}/attachments")
    public ResponseEntity<List<ChatMessageResponse>> getChatAttachments(
            @PathVariable String chatId,
            @RequestParam String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "3") int size){
        log.info("Fetching attachments of type '{}' for chatId '{}'", type, chatId);
        
        org.springframework.data.domain.Pageable pageable=PageRequest.of(page, size, Sort.by("timestamp").descending());   
        
        List<ChatMessageResponse> attachments = chatMessageService.getChatAttachments(chatId, type,pageable);
        return ResponseEntity.ok(attachments);
    }
    
    
//   searching sidebar for Groups and Private chats
    
    @GetMapping("/overview/search")
    public ResponseEntity<List<Map<String, Object>>> searchChatOverview(
            @RequestParam String employeeId,
            @RequestParam String query,
            @RequestHeader ("Authorization") String token) {
    	
        log.info("Searching chat overview for employeeId: {} with query: {}", employeeId, query);
        customFeignContext.setToken(token);
        List<Map<String, Object>> searchResults = chatMessageService.searchChatOverview(employeeId, query);
        return ResponseEntity.ok(searchResults);
    }
    
}
    
   