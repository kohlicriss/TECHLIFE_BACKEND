package com.example.OfferLetter.Repository;

import com.example.OfferLetter.Entity.OfferLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfferRepository extends JpaRepository<OfferLetter, Long> {


    Optional<OfferLetter> findByEmployeeId(String employeeId);
}
