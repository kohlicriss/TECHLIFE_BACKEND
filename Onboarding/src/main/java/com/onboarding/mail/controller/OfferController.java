package com.onboarding.mail.controller;

import javax.security.sasl.SaslException;

import com.onboarding.mail.dto.CandidateOnboardingResponse;
import com.onboarding.mail.dto.OnboardingRequest;
import com.onboarding.mail.dto.SimplePageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.onboarding.mail.entity.Candidate;
import com.onboarding.mail.entity.OfferToken;
import com.onboarding.mail.repo.CandidateRepo;
import com.onboarding.mail.service.CandidateService;
import com.onboarding.mail.service.EmailService;
import com.onboarding.mail.service.TokenService;

import ch.qos.logback.core.subst.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/offer")
@CrossOrigin("*")
public class OfferController {
	
	@Autowired
	private CandidateService candidateService;
	
	@Autowired
	private TokenService tokenService;
	
	@Autowired
	private EmailService emailService;
	
	
	@PostMapping("/sendMail")
	public String sendOffer(@RequestBody Candidate candidate) {
		
		Candidate savedCandidate = candidateService.saveCandidate(candidate);
		log.info("candidate saved");
		
		OfferToken token = tokenService.createToken(savedCandidate);
		log.info("token created");
		
		emailService.sendEmail(savedCandidate, token);
		log.info("mail send");
		
		return "Mail sent succesfully to "+candidate.getEmail();
	}
	
    @GetMapping("/view")
    public ResponseEntity<?> viewOffer(@RequestParam String token) {

        OfferToken offerToken = tokenService.validateToken(token);

        Candidate c = offerToken.getCandidate();

        return ResponseEntity.ok(
                "validLink"
        );
    }



	@PostMapping(value = "/submit-onboarding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> submitOnboarding(
			@RequestParam String token,
			@RequestPart("data") OnboardingRequest data,
			@RequestPart MultipartFile resume,
			@RequestPart MultipartFile aadhaar,
			@RequestPart MultipartFile pan,
			@RequestPart MultipartFile photo,
			@RequestPart(required = false) MultipartFile sscMemo,
			@RequestPart(required = false) MultipartFile interMemo,
			@RequestPart(required = false) MultipartFile degreeDoc,
			@RequestPart MultipartFile introVideo
	)
	{
		candidateService.processOnboarding(
				token,
				data.getFullName(),
				data.getDob(),
				data.getPhone(),
				data.getGraduated(),
				data.getCollegeName(),
				data.getPassingYear(),
				data.getAddress(),
				resume,
				aadhaar,
				pan,
				photo,
				sscMemo,
				interMemo,
				degreeDoc,
				introVideo
		);

		return ResponseEntity.ok("Onboarding completed successfully");
	}
	@GetMapping("/onboarded-candidates")
	public ResponseEntity<?> getAllOnboardedCandidates(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size
	) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
		Page<CandidateOnboardingResponse> pageResult =
				candidateService.getAllOnboardedCandidates(pageable);

		SimplePageResponse<CandidateOnboardingResponse> response =
				new SimplePageResponse<>(
						pageResult.getContent(),
						pageResult.getTotalElements(),
						pageResult.getTotalPages(),
						pageResult.isEmpty()
				);

		return ResponseEntity.ok(response);
	}






}
