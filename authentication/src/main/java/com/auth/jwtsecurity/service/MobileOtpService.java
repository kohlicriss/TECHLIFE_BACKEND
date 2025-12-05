package com.auth.jwtsecurity.service;

import com.auth.jwtsecurity.dto.TokenPair;
import com.auth.jwtsecurity.model.OtpStore;
import com.auth.jwtsecurity.model.User;
import com.auth.jwtsecurity.repository.OtpRepo;
import com.auth.jwtsecurity.repository.UserRepository;
import com.auth.jwtsecurity.util.RsaKeyUtil;
import jakarta.annotation.PostConstruct;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MobileOtpService {

    private final SnsClient snsClient;
    private final UserRepository userRepository;
    private final OtpRepo otpRepo;
    private final RsaKeyUtil rsaKeyUtil;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;

    @Autowired
    public MobileOtpService(SnsClient snsClient, UserRepository userRepository,
                            OtpRepo otpRepo, RsaKeyUtil rsaKeyUtil, JwtService jwtService, JavaMailSender mailSender) {
        this.snsClient = snsClient;
        this.userRepository = userRepository;
        this.otpRepo = otpRepo;
        this.rsaKeyUtil = rsaKeyUtil;
        this.jwtService = jwtService;
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void initKeys() throws Exception {
        this.publicKey = rsaKeyUtil.loadPublicKey("classpath:keys/public_key.pem");
        this.privateKey = rsaKeyUtil.loadPrivateKey("classpath:keys/private_key.pem");
    }

    public String sendOtpToUserPhone(String employeeId, String phoneNumber) throws Exception {
        if ((Objects.equals(employeeId, "np") || employeeId.isEmpty()) && (Objects.equals(phoneNumber, "np") || phoneNumber.isEmpty())) {
            throw new BadRequestException("Either employeeId or phoneNumber must be provided");
        }

        Optional<User> user;
        if (employeeId.equals("np")) {
            user = userRepository.findByPhoneNumber(phoneNumber);
        } else {
            user = userRepository.findByUsername(employeeId.toLowerCase());
        }

        if (user.isEmpty()) throw new BadRequestException("No user found with the given details");

        User userDetails = user.get();
        if (userDetails.getPhoneNumber() == null) {
            throw new BadRequestException("Phone number not bound with the employee, please contact management");
        }

        String id = sendOTP(userDetails, "PHONE");
        return rsaKeyUtil.rsaEncrypt(publicKey, id);
    }

    public String sendOtpToUserEmail(String employeeId, String email) throws Exception {
        if ((Objects.equals(employeeId, "np") || employeeId.isEmpty()) && (Objects.equals(email, "np") || email.isEmpty())) {
            throw new BadRequestException("Either employeeId or email must be provided");
        }

        Optional<User> user;
        if (employeeId.equals("np")) {
            user = userRepository.findByEmail(email);
        } else {
            user = userRepository.findByUsername(employeeId.toLowerCase());
        }

        if (user.isEmpty()) throw new BadRequestException("No user found with the given details");

        User userDetails = user.get();
        if (userDetails.getEmail() == null) {
            throw new BadRequestException("Given Email not bound with the employee, please contact management");
        }

        String id = sendOTP(userDetails, "EMAIL");
        return rsaKeyUtil.rsaEncrypt(publicKey, id);
    }

    public TokenPair verifyOTP(String givenOtp, String key) throws Exception {
        String id = rsaKeyUtil.rsaDecrypt(privateKey, key);
        Optional<OtpStore> otp = otpRepo.findById(id);
        if (otp.isEmpty()) throw new BadRequestException("Otp not sent");
        OtpStore otpDetails = otp.get();
        if (otpDetails.getExpiryTime().isBefore(Instant.now())) throw new BadRequestException("Otp expired");
        if (otpDetails.getVerified() == true) throw new BadRequestException("Otp already verified");
        if (!otpDetails.getOtp().equals(givenOtp)) throw new BadRequestException("Otp not match");
        otpDetails.setVerified(true);
        OtpStore saved = otpRepo.save(otpDetails);
        User user = saved.getEmployeeID();
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // encoded is fine
                .roles(String.valueOf(user.getRole()).replaceFirst("^ROLE_", ""))
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return jwtService.generateTokenPair(authentication);
    }
    public String OtpSender(String employeeId, String email, String phone) throws Exception {

        boolean noEmployeeId = (employeeId == null || employeeId.equals("np") || employeeId.isBlank());
        boolean noEmail      = (email == null || email.equals("np") || email.isBlank());
        boolean noPhone      = (phone == null || phone.equals("np") || phone.isBlank());

        if (noEmployeeId && noEmail && noPhone) {
            throw new BadRequestException("At least one of employeeId, email or phone must be provided");
        }

        Optional<User> userOptional = Optional.empty();

        if (!noEmployeeId) {
            userOptional = userRepository.findByUsername(employeeId.toLowerCase().trim());
        } else if (!noEmail) {
            userOptional = userRepository.findByEmail(email.trim());
        } else if (!noPhone) {
            userOptional = userRepository.findByPhoneNumber(phone.trim());
        }

        if (userOptional.isEmpty()) {
            throw new BadRequestException("No user found with the provided details");
        }

        User user = userOptional.get();

        boolean sendToEmail = !noEmail;
        boolean sendToPhone = !noPhone;
        String otpType;

        if (sendToEmail) {
            if (user.getEmail() == null) {
                throw new BadRequestException("Email not bound with this employee");
            }
            otpType = "EMAIL";
        } else if (sendToPhone) {
            if (user.getPhoneNumber() == null) {
                throw new BadRequestException("Phone number not bound with this employee");
            }
            otpType = "PHONE";
        } else {
            throw new BadRequestException("Either email or phone must be provided to send OTP");
        }

        String id = sendOTP(user, otpType);

        return rsaKeyUtil.rsaEncrypt(publicKey, id);
    }


    public String sendOTP(User user, String Option) {
        String number = user.getPhoneNumber();
        String email = user.getEmail();
        String appName = "Techlife";
        String otpCode = generateNumericOtp(6);
        String otpMessage = String.format(
                "Dear customer, Your One Time Password is (%s) to log in to your %s account. This OTP will be valid for the next 5 mins.",
                otpCode, appName
        );
        String messageId = null;
        if (Option.equals("PHONE")) messageId = sendSms(number, otpMessage);
        if (Option.equals("EMAIL")) messageId = sendEmail(email, otpMessage);
        OtpStore otpStore = OtpStore.builder()
                .otp(otpCode)
                .messageId(messageId)
                .employeeID(user)
                .sentTime(LocalDateTime.now())
                .expiryTime(Instant.now().plusSeconds(180))
                .verified(false)
                .build();
        otpRepo.save(otpStore);
        return messageId;
    }

    private static String generateNumericOtp(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("OTP length must be at least 1.");
        }
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public String sendSms(String phoneNumber, String message) {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("AWS.SNS.SMS.SMSType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("Transactional") // "Transactional" or "Promotional"
                        .build()
        );

        PublishRequest request = PublishRequest.builder()
                .message(message)
                .phoneNumber(phoneNumber)
                .messageAttributes(messageAttributes)
                .build();

        PublishResponse response = snsClient.publish(request);
        System.out.println("SMS sent with Message ID: " + response.messageId());
        return response.messageId();
    }

    private String sendEmail(String email, String message) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Password Reset OTP - TECH-LIFE");
        msg.setText(message);

        mailSender.send(msg);
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}
