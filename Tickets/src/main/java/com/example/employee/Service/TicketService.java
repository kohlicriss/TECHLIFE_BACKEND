package com.example.employee.Service;

import com.example.employee.Repository.TicketReplyRepository;
import com.example.employee.Repository.TicketRepository;
import com.example.employee.Repository.UserRepository;
import com.example.employee.WebSocket.TicketWebSocketHandler;
import com.example.employee.WebSocket.UnifiedTicketWebSocketHandler;
import com.example.employee.client.NotificationClient;
import com.example.employee.dto.PageResponseDTO;
import com.example.employee.dto.TicketDTO;
import com.example.employee.dto.TicketReplyDTO;
import com.example.employee.entity.NotificationRequest;
import com.example.employee.entity.Roles;
import com.example.employee.entity.Ticket;
import com.example.employee.entity.TicketReply;
import com.example.employee.entity.User;
import com.example.employee.handlers.*;
import com.example.employee.redis.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketReplyRepository ticketReplyRepository;
    private final UserRepository userRepository;
    private final RedisMessagePublisher redisPublisher;
    private final NotificationClient notificationClient;
    private final UnifiedTicketWebSocketHandler ticketWebSocketHandler;
   // private static final Logger log=LoggerFactory.getLogger(TicketService.class);
   private final AsyncTicketHelper asyncTaskService;
   private final TicketNotificationService ticketNotificationService;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = {
            @CacheEvict(value = "ticketsCache", allEntries = true)
    })
    public Ticket createTicket(String employeeId, String ticketId, String title,
                               String description, String priority, Roles roles) {
        try {
            if (ticketId == null || ticketId.trim().isEmpty()) {
                long count = ticketRepository.count() + 1;
                ticketId = String.format("TICK%05d", count);
            }

            Ticket ticket = Ticket.builder()
                    .ticketId(ticketId)
                    .title(title)
                    .description(description)
                    .status("OPEN")
                    .sentAt(LocalDateTime.now())
                    .employeeId(employeeId)
                    .roles(roles)
                    .priority(priority)
                    .build();

            Ticket saved = ticketRepository.saveAndFlush(ticket);
            log.info("Ticket created with ID={}, by employee={}, assigned to role={}",
                    saved.getTicketId(), employeeId, roles);

            try {
                TicketDTO dto = convertToDto(saved);
                asyncTaskService.publishTicketToRedisAsync(dto);
                log.info("Async Redis publish triggered for ticket {}", saved.getTicketId());
            } catch (RedisAccessException exr) {
                log.error("Failed to publish ticket {} to Redis: {}", ticketId, exr.getMessage(), exr);
                throw new RedisAccessException("unable to publish message to redis");
            }

            // Send ticket assignment notification
            ticketNotificationService.sendTicketAssignmentNotification(saved);

            return saved;

        } catch (TicketNotCreatedException ex) {
            log.error("Unexpected error while creating ticket: {}", ex.getMessage(), ex);
            throw new TicketNotCreatedException("Unexpected error while creating ticket");
        }
    }


@Cacheable(value = "ticketsCache", key = "#ticketId")
public Ticket getTicketById(String ticketId) {
    log.info("Fetching ticket with id={}", ticketId);

    return ticketRepository.findById(ticketId)
            .map(ticket -> {
                log.info("Ticket found: id={}, title={}", ticket.getTicketId(), ticket.getTitle());
                return ticket;
            })
            .orElseThrow(() -> {
                log.warn("No ticket found with id={}", ticketId);
                return new TicketNotFoundException("Ticket not found with id: " + ticketId);
            });
}



    public PageResponseDTO<TicketDTO> getAllTicketsPaginated(int page, int size) {
        log.info("Fetching tickets page={}, size={}", page, size);

        try {
            Page<Ticket> ticketPage = ticketRepository.findAll(
                    PageRequest.of(page, size, Sort.by("sentAt").descending())
            );

            if (ticketPage.isEmpty()) {
                log.warn("No tickets found for page={}, size={}", page, size);
                throw new UnableToFetchTicketsException("No tickets available to display.");
            }

            List<TicketDTO> content = ticketPage.getContent()
                    .stream()
                    .map(this::convertToDto)
                    .toList();

            log.info("Fetched {} tickets out of total={}, totalPages={}",
                    content.size(), ticketPage.getTotalElements(), ticketPage.getTotalPages());

            return new PageResponseDTO<>(
                    content,
                    page,
                    size,
                    ticketPage.getTotalElements(),
                    ticketPage.getTotalPages()
            );
        } catch (Exception e) {
            log.error("Error fetching tickets page={}, size={}: {}", page, size, e.getMessage(), e);
            throw new UnableToFetchTicketsException("Unable to fetch tickets at this time. Please try again later.", e);
        }
    }



    public PageResponseDTO<TicketDTO> getTicketsByRoleOnlyPaginated(Roles roles, int page, int size) {
     try {
         Page<Ticket> ticketPage = ticketRepository.findByRoles(roles, PageRequest.of(page, size, Sort.by("sentAt").descending()));
         List<TicketDTO> content = ticketPage.getContent().stream()
                 .map(this::convertToDto)
                 .toList();

         log.info("Fetched by role {} out of total={}, totalPages={}", content.size(), ticketPage.getTotalElements(), ticketPage.getTotalPages());

         return new PageResponseDTO<>(content, page, size, ticketPage.getTotalElements(), ticketPage.getTotalPages());
     }catch (Exception ex){
         log.error("Error fetching tickets roles={} ", roles,  ex.getMessage(), ex);
         throw new UnauthorizedAccessException("You do not have access",ex);

     }
    }



    public PageResponseDTO<TicketReply> getRepliesByTicketIdPaginated(String ticketId, int page, int size) {
        Page<TicketReply> replyPage = ticketReplyRepository.findByTicket_TicketId(ticketId, PageRequest.of(page, size, Sort.by("repliedAt").descending()));
        return new PageResponseDTO<>(replyPage.getContent(), page, size, replyPage.getTotalElements(), replyPage.getTotalPages());
    }



    @Cacheable(value = "ticketsCache", key = "'replies-' + #ticketId + '-' + #page + '-' + #size")
    public Page<TicketReplyDTO> getRepliesByTicketId(String ticketId, int page, int size) {
     try {
         Pageable pageable = PageRequest.of(page, size, Sort.by("repliedAt").ascending());
         Page<TicketReply> replies = ticketReplyRepository.findByTicket_TicketId(ticketId, pageable);

         return replies.map(reply -> {
             TicketReplyDTO dto = new TicketReplyDTO();
             dto.setTicketId(ticketId);
             dto.setReplyText(reply.getReplyText());
             dto.setRepliedBy(reply.getRepliedBy());
             dto.setEmployeeId(reply.getEmployeeId());
             dto.setRepliedAt(reply.getRepliedAt().toString());
             dto.setRoles(reply.getRoles());
             return dto;
         });
     } catch (Exception e) {
         throw new TicketReplyException("unable to fetch replies for your ticket" , e);
     }
    }
  public PageResponseDTO<TicketDTO> getTicketsByEmployeePaginated(String employeeId, int page, int size) {
    try {
        Page<Ticket> ticketPage = ticketRepository.findByEmployeeId(
                employeeId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"))
        );

        List<TicketDTO> content = ticketPage.getContent()
                .stream()
                .map(ticket -> convertToDto(ticket))
                .collect(Collectors.toList());

        return new PageResponseDTO<>(
                content,
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages()
        );
    } catch (Exception e) {
        throw new UnableToFetchTicketsException("Unable to fetch tickets by employeeId",e);

    }
}


    public PageResponseDTO<TicketReplyDTO> getRepliesByTicketIdPaginatedDTO(String ticketId, int page, int size) {
       try {
           Page<TicketReply> replyPage = ticketReplyRepository.findByTicket_TicketId(ticketId,
                   PageRequest.of(page, size, Sort.by("repliedAt").descending()));

           List<TicketReplyDTO> content = replyPage.getContent().stream().map(reply -> {
               TicketReplyDTO dto = new TicketReplyDTO();
               dto.setTicketId(ticketId);
               dto.setReplyText(reply.getReplyText());
               dto.setRepliedBy(reply.getRepliedBy());
               dto.setEmployeeId(reply.getEmployeeId());
               dto.setRepliedAt(reply.getRepliedAt().toString());
               dto.setRoles(reply.getRoles());
               return dto;
           }).toList();

           return new PageResponseDTO<>(content, page, size, replyPage.getTotalElements(), replyPage.getTotalPages());
       } catch (Exception e) {
           throw new TicketReplyException("Unable to fetch replies " , e);
       }
    }



    public TicketDTO    convertToDto(Ticket ticket) {
        if (ticket == null) throw new IllegalArgumentException("Ticket cannot be null");

        TicketDTO dto = new TicketDTO();
        dto.setTicketId(ticket.getTicketId());
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setStatus(Optional.ofNullable(ticket.getStatus()).orElse(""));
        dto.setPriority(ticket.getPriority());
        dto.setEmployeeId(ticket.getEmployeeId());
        dto.setRoles(ticket.getRoles());
        dto.setSentAt(ticket.getSentAt().toString());


//        if (ticket.getReplies() != null) {
//            List<TicketReplyDTO> replies = ticket.getReplies().stream()
//                    .filter(Objects::nonNull)
//                    .map(reply -> {
//                        TicketReplyDTO r = new TicketReplyDTO();
//                        r.setTicketId(reply.getTicket().getTicketId());
//                        r.setReplyText(reply.getReplyText());
//                        r.setRepliedBy(reply.getRepliedBy());
//                        r.setStatus(Optional.ofNullable(reply.getStatus()).orElse(""));
//                        r.setEmployeeId(reply.getEmployeeId());
//                        r.setRoles(reply.getRoles());
//                        r.setRepliedAt(reply.getRepliedAt().toString());
//                        return r;
//                    }).toList();
//            dto.setReplies(replies);
//        }

        return dto;
    }

   @Caching(evict = {
        @CacheEvict(value = "ticketsCache", key = "'replies-' + #dto.ticketId + '-*'", beforeInvocation = true),
        @CacheEvict(value = "ticketsCache", allEntries = true, beforeInvocation = true)
})
public void saveReply(TicketReplyDTO dto) {
    try {
        Ticket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + dto.getTicketId()));

        TicketReply reply = new TicketReply();
        reply.setTicket(ticket);
        reply.setReplyText(dto.getReplyText());
        reply.setRepliedBy(dto.getRepliedBy());
        reply.setRepliedAt(dto.getRepliedAt() != null ? LocalDateTime.parse(dto.getRepliedAt()) : LocalDateTime.now());
        reply.setStatus(dto.getStatus());
        reply.setRoles(dto.getRoles());
        reply.setEmployeeId(dto.getEmployeeId());

        TicketReply savedReply = ticketReplyRepository.save(reply);

        if (dto.getStatus() != null) {
            ticket.setStatus(dto.getStatus());
            ticketRepository.save(ticket);
        }

        TicketReplyDTO responseDto = new TicketReplyDTO();
        responseDto.setTicketId(ticket.getTicketId());
        responseDto.setReplyText(savedReply.getReplyText());
        responseDto.setRepliedBy(savedReply.getRepliedBy());
        responseDto.setEmployeeId(savedReply.getEmployeeId());
        responseDto.setStatus(savedReply.getStatus());
        responseDto.setRoles(savedReply.getRoles());
        responseDto.setRepliedAt(savedReply.getRepliedAt().toString());

        // BROADCAST VIA WEBSOCKET
        log.info("📢 Broadcasting reply via WebSocket for ticketId={}", dto.getTicketId());
        ticketWebSocketHandler.broadcastToTicket(responseDto);
        
        log.info("Reply saved and broadcasted successfully for ticketId={}", dto.getTicketId());

    } catch (RuntimeException e) {
        log.error("Error while saving reply for ticketId={}, cause={}", dto.getTicketId(), e.getMessage(), e);
        throw new TicketReplySaveException("Failed to save reply for ticketId=" + dto.getTicketId(), e);
    } catch (Exception e) {
        log.error("Unexpected error occurred while saving reply for ticketId={}, cause={}", dto.getTicketId(), e.getMessage(), e);
        throw new TicketReplySaveException("Unexpected error while saving reply for ticketId=" + dto.getTicketId(), e);
    }
}
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TicketReply replyToTicket(String ticketId, String replyText, String repliedBy,
                                     String status, String employeeId, Roles roles) {
        log.info("Replying to ticketId={} by employeeId={} with role={}", ticketId, employeeId, roles);

        try {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> {
                        log.error("Ticket not found for ticketId={}", ticketId);
                        return new RuntimeException("Ticket not found with id=" + ticketId);
                    });

            // Store old status for notification
            String oldStatus = ticket.getStatus();

            TicketReply reply = TicketReply.builder()
                    .ticket(ticket)
                    .replyText(replyText)
                    .repliedBy(repliedBy)
                    .employeeId(employeeId)
                    .status(status)
                    .roles(roles)
                    .repliedAt(LocalDateTime.now())
                    .build();

            TicketReply savedReply = ticketReplyRepository.saveAndFlush(reply);
            ticket.getReplies().add(savedReply);

            // Update ticket status if provided
            if (status != null && !status.isEmpty() && !status.equals(ticket.getStatus())) {
                log.info("Updating ticketId={} status from {} to {}", ticketId, ticket.getStatus(), status);
                ticket.setStatus(status);
                ticketRepository.saveAndFlush(ticket);

                // Send status update notification
                if (!status.equals(oldStatus)) {
                    ticketNotificationService.sendTicketStatusUpdateNotification(ticket, repliedBy, oldStatus, status);
                }
            }

            // Create the DTO for WebSocket broadcast
            TicketReplyDTO dto = new TicketReplyDTO();
            dto.setTicketId(ticket.getTicketId());
            dto.setReplyText(savedReply.getReplyText());
            dto.setRepliedBy(savedReply.getRepliedBy());
            dto.setEmployeeId(savedReply.getEmployeeId());
            dto.setStatus(savedReply.getStatus());
            dto.setRoles(savedReply.getRoles());
            dto.setRepliedAt(savedReply.getRepliedAt().toString());

            // BROADCAST VIA WEBSOCKET
            log.info("📢 Broadcasting reply via WebSocket for ticketId={}", ticketId);
            ticketWebSocketHandler.broadcastToTicket(dto);
            asyncTaskService.broadcastReplyAsync(dto);

            // SEND APPROPRIATE NOTIFICATIONS BASED ON ROLE
            if (roles == Roles.ROLE_ADMIN || roles == Roles.ROLE_HR || roles == Roles.ROLE_MANAGER) {
                // Admin/Manager/HR replying - notify employee
                if (!employeeId.equals(ticket.getEmployeeId())) {
                    ticketNotificationService.sendAdminReplyNotification(ticket, repliedBy, replyText);
                }
            } else if (roles == Roles.ROLE_EMPLOYEE) {
                // Employee replying - notify assigned role users
                ticketNotificationService.sendEmployeeReplyNotification(ticket, repliedBy, replyText);
            }

            // Send resolved notification if ticket is marked as resolved
            if ("Resolved".equalsIgnoreCase(status) &&
                    (roles == Roles.ROLE_ADMIN || roles == Roles.ROLE_HR || roles == Roles.ROLE_MANAGER)) {
                ticketNotificationService.sendTicketResolvedNotification(ticket, repliedBy);
            }

            log.info("Reply process completed for ticketId={}, replyId={}", ticketId, savedReply.getId());
            return savedReply;

        } catch (Exception e) {
            log.error("Failed to reply to ticketId={} due to error: {}", ticketId, e.getMessage(), e);
            throw new TicketReplyException("Unable to reply to ticket with id: " + ticketId, e);
        }
    }

}
