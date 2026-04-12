package net.engineerAnsh.BankApplication.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycStatusResponse;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycSubmissionRequest;
import net.engineerAnsh.BankApplication.Security.UserDetails.CustomUserDetails;
import net.engineerAnsh.BankApplication.services.kyc.KycService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/status/view/{referenceId}")
    public ResponseEntity<KycStatusResponse> getStatus(
            @PathVariable String referenceId) {
        return ResponseEntity.ok(
                kycService.getKycStatus(referenceId)
        );
    }

    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getKycStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String referenceId) {
        KycStatusResponse userKycStatus = kycService.getKycStatusOfUser(user.getUserId(),referenceId);
        return ResponseEntity.ok().body(userKycStatus);
    }


}