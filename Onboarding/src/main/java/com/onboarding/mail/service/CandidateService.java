package com.onboarding.mail.service;

import com.onboarding.mail.dto.CandidateOnboardingResponse;
import com.onboarding.mail.entity.Candidate;
import com.onboarding.mail.entity.CandidateOnboardingDetails;
import com.onboarding.mail.entity.OfferToken;
import com.onboarding.mail.repo.CandidateRepo;
import com.onboarding.mail.repo.OnboardingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepo candidateRepo;
    private final TokenService tokenService;
    private final S3StorageService s3Service;
    private final OnboardingRepository onboardingRepo;

    public Candidate saveCandidate(Candidate candidate) {
        return candidateRepo.save(candidate);
    }

    public void processOnboarding(
            String token,
            String fullName,
            String dob,
            String phone,
            Boolean graduated,
            String collegeName,
            Integer passingYear,
            String address,
            MultipartFile resume,
            MultipartFile aadhaar,
            MultipartFile pan,
            MultipartFile photo,
            MultipartFile sscMemo,
            MultipartFile interMemo,
            MultipartFile degreeDoc,
            MultipartFile introVideo) {

        OfferToken offerToken = tokenService.validateToken(token);
        Candidate candidate = offerToken.getCandidate();

        CandidateOnboardingDetails details = new CandidateOnboardingDetails();
        details.setCandidate(candidate);
        details.setFullName(fullName);
        details.setDob(LocalDate.parse(dob));
        details.setPhone(phone);
        details.setGraduated(graduated);
        details.setCollegeName(collegeName);
        details.setPassingYear(passingYear);
        details.setAddress(address);

        details.setResumePath(
                s3Service.uploadFile(resume, candidate.getId(), "resume")
        );
        details.setAadhaarPath(
                s3Service.uploadFile(aadhaar, candidate.getId(), "aadhaar")
        );
        details.setPanPath(
                s3Service.uploadFile(pan, candidate.getId(), "pan")
        );
        details.setPhotoPath(
                s3Service.uploadFile(photo, candidate.getId(), "photo")
        );
        details.setSscMemo(
                s3Service.uploadFile(sscMemo, candidate.getId(), "tenth")
        );
        details.setInterMemo(
                s3Service.uploadFile(interMemo, candidate.getId(), "inter")
        );
        details.setIntroVideo(
                s3Service.uploadFile(introVideo, candidate.getId(), "introVideo")
        );

        if (degreeDoc != null && !degreeDoc.isEmpty()) {
            String degreeKey = Boolean.TRUE.equals(graduated)
                    ? "degree-original"
                    : "degree-recent";

            details.setDegreeDoc(
                    s3Service.uploadFile(degreeDoc, candidate.getId(), degreeKey)
            );
        }

        onboardingRepo.save(details);
        offerToken.setUsed(true);

    }

    public Page<CandidateOnboardingResponse> getAllOnboardedCandidates(Pageable pageable) {

        Page<CandidateOnboardingDetails> page =
                onboardingRepo.findAll(pageable);

        return page.map(details -> {

            CandidateOnboardingResponse dto = new CandidateOnboardingResponse();

            dto.setCandidateId(details.getCandidate().getId());
            dto.setName(details.getCandidate().getName());
            dto.setEmail(details.getCandidate().getEmail());
            dto.setRole(details.getCandidate().getRole());

            dto.setPhone(details.getPhone());
            dto.setDob(details.getDob().toString());
            dto.setGraduated(details.getGraduated());
            dto.setCollegeName(details.getCollegeName());
            dto.setPassingYear(details.getPassingYear());
            dto.setAddress(details.getAddress());

            dto.setResumeUrl(s3Service.generatePresignedUrl(details.getResumePath()));
            dto.setAadhaarUrl(s3Service.generatePresignedUrl(details.getAadhaarPath()));
            dto.setPanUrl(s3Service.generatePresignedUrl(details.getPanPath()));
            dto.setPhotoUrl(s3Service.generatePresignedUrl(details.getPhotoPath()));
            dto.setInterMemoUrl(s3Service.generatePresignedUrl(details.getInterMemo()));
            dto.setSscMemoUrl(s3Service.generatePresignedUrl(details.getSscMemo()));
            dto.setDegreeDocUrl(s3Service.generatePresignedUrl(details.getDegreeDoc()));
            dto.setIntroVideo(s3Service.generatePresignedUrl(details.getIntroVideo()));

            return dto;
        });
    }

}


