package net.engineerAnsh.BankApplication.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "KYC APIs", description = "KYC submission and verification operations")
public class KycController {

    private final KycService kycService;

    @Operation(
            summary = "Submit KYC",
            description = "Allows authenticated user to submit KYC documents"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/submit")
    public ResponseEntity<String> submitKyc(
            @AuthenticationPrincipal CustomUserDetails user, // '@AuthenticationPrincipal' This is a Spring Security feature, It injects the currently authenticated user into the controller method.

            @Parameter(description = "KYC submission request payload", required = true)
            @Valid @RequestBody KycSubmissionRequest request) throws JsonProcessingException
    {
        kycService.submitKyc(user.getUserId(), request);
        return ResponseEntity.ok("Kyc document is submitted successfully...");
    }

    @Operation(
            summary = "Get KYC Status (Public)",
            description = "Fetch KYC status using reference ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC status fetched successfully"),
            @ApiResponse(responseCode = "404", description = "KYC not found")
    })
    @GetMapping("/status/view/{referenceId}")
    public ResponseEntity<KycStatusResponse> getStatus(
            @Parameter(description = "KYC reference ID", example = "KYC123456")
            @PathVariable String referenceId) {
        return ResponseEntity.ok(
                kycService.getKycStatus(referenceId)
        );
    }


    @Operation(
            summary = "Get KYC Status (User)",
            description = "Fetch KYC status for logged-in user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "KYC status fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getKycStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "KYC reference ID", example = "KYC123456")
            @RequestParam String referenceId)
    {
        KycStatusResponse userKycStatus = kycService.getKycStatusOfUser(user.getUserId(),referenceId);
        return ResponseEntity.ok().body(userKycStatus);
    }


}