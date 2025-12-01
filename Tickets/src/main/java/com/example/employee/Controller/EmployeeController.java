package com.example.employee.Controller;

import com.example.employee.Repository.TicketRepository;
import com.example.employee.Service.TicketService;
import com.example.employee.WebSocket.TicketWebSocketHandler;
import com.example.employee.WebSocket.UnifiedTicketWebSocketHandler;
import com.example.employee.dto.PageResponseDTO;
import com.example.employee.dto.TicketDTO;
import com.example.employee.dto.TicketReplyDTO;
import com.example.employee.entity.Ticket;
import com.example.employee.entity.TicketReply;
import com.example.employee.security.CheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.management.Notification;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/ticket/employee")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final UnifiedTicketWebSocketHandler ticketWebSocketHandler;

   // @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'TEAM_LEAD','EMPLOYEE')")

    @PostMapping("/create")
    @CheckPermission("CREATE_TICKET")
    public ResponseEntity<TicketDTO> createTicket(@RequestBody TicketDTO dto) {
       Ticket savedTicket = ticketService.createTicket(
        dto.getEmployeeId(),
        null, 
        dto.getTitle(),
        dto.getDescription(),
        dto.getPriority(),
        dto.getRoles()
);
return ResponseEntity.ok(ticketService.convertToDto(savedTicket));

    }
  //employee
    @PostMapping("/tickets/{ticketId}/messages")
   // @CheckPermission("SEND_TICKET_REPLIES")
    //@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'TEAM_LEAD','EMPLOYEE')")
    public ResponseEntity<TicketReplyDTO> replyToTicket(
            @PathVariable String ticketId,
            @RequestBody TicketReplyDTO dto
    ) {
        TicketReply savedReply = ticketService.replyToTicket(
                ticketId,
                dto.getReplyText(),
                dto.getRepliedBy(),
                null,
                dto.getEmployeeId(),
                dto.getRoles()
        );

        TicketReplyDTO responseDto = new TicketReplyDTO();
        responseDto.setTicketId(savedReply.getTicket().getTicketId());
        responseDto.setReplyText(savedReply.getReplyText());
        responseDto.setRepliedBy(savedReply.getRepliedBy());
        responseDto.setRepliedAt(savedReply.getRepliedAt().toString());  
        responseDto.setEmployeeId(savedReply.getEmployeeId());
        responseDto.setRoles(savedReply.getRoles());  

        ticketWebSocketHandler.broadcastToTicket(responseDto);

        return ResponseEntity.ok(responseDto);
    }
    

    @GetMapping("/tickets")
    public ResponseEntity<PageResponseDTO<TicketDTO>> getEmployeeTickets(
            @RequestParam String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(ticketService.getTicketsByEmployeePaginated(employeeId, page, size));
    }

    //employee
    @GetMapping("/tickets/{ticketId}/messages")
//     @CheckPermission(
//            value = "VIEW_TICKET_REPLIES",
//            MatchParmName = "ticketId",
//            MatchParmFromUrl = "ticketId",
//            MatchParmForRoles = {"ROLE_HR","ROLE_MANAGER" , "ROLE_ADMIN","ROLE_EMPLOYEE" , "ROLE_TEAMLEAD"}
//    )
    //@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'TEAM_LEAD','EMPLOYEE')")
    public ResponseEntity<PageResponseDTO<TicketReplyDTO>> getTicketReplies(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(ticketService.getRepliesByTicketIdPaginatedDTO(ticketId, page, size));
    }
}
