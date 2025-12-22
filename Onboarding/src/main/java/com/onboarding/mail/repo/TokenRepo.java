package com.onboarding.mail.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onboarding.mail.entity.OfferToken;

@Repository
public interface TokenRepo extends JpaRepository<OfferToken, Long>{

	Optional<OfferToken> findByToken(String token);

}
