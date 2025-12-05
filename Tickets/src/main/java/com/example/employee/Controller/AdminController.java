package com.example.employee.Controller;

import com.example.employee.Service.TicketService;
import com.example.employee.WebSocket.TicketWebSocketHandler;
import com.example.employee.WebSocket.UnifiedTicketWebSocketHandler;
import com.example.employee.dto.PageResponseDTO;
import com.example.employee.dto.TicketDTO;
import com.example.employee.dto.TicketReplyDTO;
import com.example.employee.entity.Roles;
import com.example.employee.entity.TicketReply;
import com.example.employee.security.CheckEmployeeAccess;
import com.example.employee.security.CheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ticket/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final TicketService ticketService;
    private final UnifiedTicketWebSocketHandler ticketWebSocketHandler;


    // @GetMapping("/tickets")
    // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'TEAM_LEAD', 'EMPLOYEE')")
    // public ResponseEntity<PageResponseDTO<TicketDTO>> getAllTickets(
    //         @RequestParam(defaultValue = "0") int page,
    //         @RequestParam(defaultValue = "10") int size
    // ) {
    //     return ResponseEntity.ok(ticketService.getAllTicketsPaginated(page, size));
    // }


    @GetMapping("/tickets/employee/{employeeId}")
    @CheckPermission("VIEW_MY_TICKETS")
   // @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR','TEAM_LEAD', 'MANAGER')")
    @CheckEmployeeAccess(param = "employeeId", roles = {"ADMIN", "HR", "MANAGER" })
    public ResponseEntity<PageResponseDTO<TicketDTO>> getTicketsByEmployee(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(ticketService.getTicketsByEmployeePaginated(employeeId, page, size));
    }

   
    @GetMapping("/tickets/role/{roles}/{employeeId}")
    // @CheckPermission(
    //         value = "VIEW_ASSIGNED",
    //         MatchParmName = "employeeId",
    //         MatchParmFromUrl = "employeeId",
    //         MatchParmForRoles = {"ROLE_HR","ROLE_MANAGER","ROLE_ADMIN"}
    // )
   // @PreAuthorize("hasAnyRole('ADMIN', 'HR','MANAGER')")
    public ResponseEntity<PageResponseDTO<TicketDTO>> getTicketsByRoleOnly(
            @PathVariable String roles,
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Roles roleEnum;
        try {
            roleEnum = Roles.valueOf(roles.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new PageResponseDTO<TicketDTO>(Collections.emptyList(), page, size, 0, 0));
        }
        return ResponseEntity.ok(ticketService.getTicketsByRoleOnlyPaginated(roleEnum, page, size));
    }


    //admin
    @GetMapping("/tickets/{ticketId}/reply")
   //@CheckPermission("GET_REPLY")
    // @CheckPermission(
    //         value = "GET_REPLY",
    //         MatchParmName = "ticketId",
    //         MatchParmFromUrl = "ticketId",
    //         MatchParmForRoles = {"ROLE_HR","ROLE_MANAGER" , "ROLE_ADMIN"}
    // )
    //@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_HR', 'ROLE_MANAGER')")
    public ResponseEntity<PageResponseDTO<TicketReplyDTO>> getReplies(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ticketService.getRepliesByTicketIdPaginatedDTO(ticketId, page, size));
    }


   //admin
    @PutMapping("/tickets/{ticketId}/reply")

    // @CheckPermission(
    //         value = "REPLY_TICKET",
    //         MatchParmName = "ticketId",
    //         MatchParmFromUrl = "ticketId",
    //         MatchParmForRoles = {"ROLE_HR","ROLE_MANAGER" ,"ROLE_ADMIN"}
    // )
    //@PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN', 'HR', 'TEAM_LEAD','MANAGER')")
    //@CheckEmployeeAccess(param = "ticketId", roles = {"ADMIN", "HR", "MANAGER"})
    public ResponseEntity<?> replyToTicket(
            @PathVariable String ticketId,
            @RequestBody Map<String, String> req) {

        try {
            String replyText = req.get("replyText");
            String repliedBy = req.get("repliedBy");
            String status = req.get("status");
            String employeeId = req.get("employeeId");
            String roleStr = req.get("roles");

            if ((replyText == null || replyText.trim().isEmpty()) &&
                    (status == null || status.trim().isEmpty())) {
                return ResponseEntity.badRequest().body("Either reply text or status is required");
            }

            if (roleStr == null || roleStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Role is required");
            }

            Roles roleEnum;
            try {
                roleEnum = Roles.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid role: " + roleStr);
            }

            TicketReply savedReply = ticketService.replyToTicket(
                    ticketId, replyText, repliedBy, status, employeeId, roleEnum
            );

            TicketReplyDTO dto = new TicketReplyDTO();
            dto.setTicketId(ticketId);
            dto.setReplyText(savedReply.getReplyText());
            dto.setRepliedBy(savedReply.getRepliedBy());
            dto.setEmployeeId(savedReply.getEmployeeId());
            dto.setStatus(savedReply.getStatus());
            dto.setRoles(savedReply.getRoles());
            dto.setRepliedAt(savedReply.getRepliedAt().toString());

            ticketWebSocketHandler.broadcastToTicket(dto);

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Something went wrong: " + e.getMessage());
        }
    }
}
