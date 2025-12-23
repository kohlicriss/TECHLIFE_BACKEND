package com.onboarding.mail.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class OfferToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String token;

    private LocalDateTime expiryTime;
    private boolean used = false;


    @OneToOne
    @JoinColumn(
            name = "candidate_id",
            nullable = false
    )
    private Candidate candidate;

}
