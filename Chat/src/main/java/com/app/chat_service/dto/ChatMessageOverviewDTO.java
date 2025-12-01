package com.app.chat_service.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor // <-- ADD THIS
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ChatMessageOverviewDTO {
    private Long messageId;
    private String time;
    private String sender;
    private String receiver;
    private String date;
    private String type;
    private String kind;
    private String isSeen;
    private String content;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer duration;
    private ReplyInfoDTO replyTo;
 
    private Boolean forwarded;
    private String forwardedFrom;
    
    @JsonProperty("isEdited")
    private boolean isEdited;
}