package com.onboarding.mail.service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.onboarding.mail.exceptionHandler.InvalidTokenException;
import com.onboarding.mail.exceptionHandler.TokenExpiredException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.onboarding.mail.entity.Candidate;
import com.onboarding.mail.entity.OfferToken;
import com.onboarding.mail.repo.TokenRepo;

@Service
public class TokenService {
	
	@Autowired
	private TokenRepo tokenRepo;

	public OfferToken createToken(Candidate candidate) {
		
		OfferToken offerToken = new OfferToken();
		offerToken.setCandidate(candidate);
		offerToken.setExpiryTime(LocalDateTime.now().plusHours(24));
		offerToken.setToken(UUID.randomUUID().toString());
		
		return tokenRepo.save(offerToken);
	}
	
	 public OfferToken validateToken(String token) {
	        OfferToken offerToken = tokenRepo.findByToken(token)
	                .orElseThrow(() -> new InvalidTokenException("Invalid Link"));

		 if (offerToken.isUsed()) {
			 throw new TokenExpiredException("This onboarding link has already been used");
		 }
	        if (offerToken.getExpiryTime().isBefore(LocalDateTime.now())) {
	            throw new TokenExpiredException("Link Expired");
	        }
	        return offerToken;
	    }

}
