package net.engineerAnsh.BankApplication.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycReviewRequest;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycSubmissionRequest;
import net.engineerAnsh.BankApplication.Entity.KycVerification;
import net.engineerAnsh.BankApplication.Security.UserDetails.CustomUserDetails;
import net.engineerAnsh.BankApplication.Services.KycService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final KycService kycService;

    @PostMapping("/submit")
    public ResponseEntity<String> submitKyc(
            @AuthenticationPrincipal CustomUserDetails user, // '@AuthenticationPrincipal' This is a Spring Security feature, It injects the currently authenticated user into the controller method.
            @Valid @RequestBody KycSubmissionRequest request) throws JsonProcessingException {

        kycService.submitKyc(user.getUserId(), request);
        return ResponseEntity.ok("Kyc document is submitted successfully...");
    }

    @GetMapping("/status")
    public ResponseEntity<?> getKycStatus(
            @AuthenticationPrincipal CustomUserDetails user) {

        KycVerification userKyc = kycService.getUserKyc(user.getUserId());
        return ResponseEntity.ok().body(userKyc.getStatus());
    }

    @GetMapping("/admin/pending")
    public List<KycVerification> getPendingKyc() {
        return kycService.getPendingKyc();
    }

    @PostMapping("/admin/{kycId}/review")
    public ResponseEntity<String> reviewKyc(
            @PathVariable Long kycId,
            @AuthenticationPrincipal CustomUserDetails admin,
            @Valid @RequestBody KycReviewRequest request) throws JsonProcessingException {

        kycService.reviewKyc(kycId, admin.getUsername(), request);
        return ResponseEntity.ok("Documents are successfully reviewed...");
    }

}