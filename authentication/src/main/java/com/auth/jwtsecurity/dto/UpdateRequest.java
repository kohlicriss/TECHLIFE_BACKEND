package com.auth.jwtsecurity.dto;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateRequest {
  private String fullName = null;
  private String username = null;
  private String password = null;
  private String role = null;
  private String phoneNumber = null;
  private String email = null;
  private String tenantId = "TECHLIFE";
  private boolean enabled =  true;
  private LocalDateTime createdAt = null;
  private LocalDateTime updatedAt = LocalDateTime.now();
  private boolean accountNonLocked = true;
}
