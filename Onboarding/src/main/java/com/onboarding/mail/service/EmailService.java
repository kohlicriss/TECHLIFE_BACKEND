package com.onboarding.mail.service;

import com.onboarding.mail.entity.Candidate;
import com.onboarding.mail.entity.OfferToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.onboarding-url:https://techlife.anasolconsultancyservices.com/offer/view}")
    private String onboardingBaseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(Candidate candidate, OfferToken token) {

        String onboardingLink = onboardingBaseUrl + "?token=" + token.getToken();

        String body = """
                Dear %s,

                Congratulations!

                We are pleased to inform you that you have been selected for the position of %s at ACS Pvt Ltd.
                We are excited to have you join our team and look forward to a successful journey together.

                To proceed with the onboarding process, please complete your onboarding by clicking the link below:

                %s

                Required Documents:
                • Government-issued ID proof (Aadhaar / Passport / PAN Card)
                • Scanned copies of educational certificates (10th, 12th, Graduation – all semesters)
                • Passport-size photograph
                • Updated resume
                • Any additional certifications or training documents (if applicable)

                Please ensure all documents are uploaded accurately.
                While reporting to the office, kindly bring Xerox copies of all the above documents.

                NOTE:
                If your onboarding submission is not completed by the end of the day,
                your offer may be revoked.

                For any queries, contact:
                hr@anasolconsultancyservices.com
                9032091726

                Best Regards,
                HR Associate
                ACS Pvt Ltd
                """.formatted(
                candidate.getName(),
                candidate.getRole(),
                onboardingLink
        );

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromEmail);   // ✅ ONLY TECHNICAL FIX
        mail.setTo(candidate.getEmail());
        mail.setSubject(" Onboarding Form – ACS Pvt Ltd");
        mail.setText(body);

        log.info("Sending onboarding email to {}", candidate.getEmail());
        mailSender.send(mail);
    }
}