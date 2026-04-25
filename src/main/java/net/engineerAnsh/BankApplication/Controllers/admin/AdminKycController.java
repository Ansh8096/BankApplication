package net.engineerAnsh.BankApplication.Controllers.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycReviewRequest;
import net.engineerAnsh.BankApplication.Dto.Kyc.KycStatusResponse;
import net.engineerAnsh.BankApplication.Security.UserDetails.CustomUserDetails;
import net.engineerAnsh.BankApplication.services.kyc.KycService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')") // hasRole("ADMIN") → ROLE_ADMIN → MATCH ...
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Kyc APIs", description = "Administrative operations for KYC")
public class AdminKycController { // Only users that have role as: "ROLE_ADMIN, will be able to access these end points"

    private final KycService kycService;

    @Operation(summary = "Get pending KYC", description = "Fetch all pending KYC requests")
    @GetMapping("/api/v1/kyc/pending")
    public List<KycStatusResponse> getPendingKyc() {
        return kycService.getPendingKyc();
    }

    @Operation(summary = "Review KYC", description = "Approve or reject KYC documents")
    @PostMapping("api/v1/kyc/review")
    public ResponseEntity<String> reviewKyc(
            @Parameter(description = "KYC ID") @RequestParam String kycId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails admin,

            @Parameter(description = "KYC review request body")
            @Valid @RequestBody KycReviewRequest request) throws JsonProcessingException {

        kycService.reviewKyc(kycId, admin.getUsername(), request);
        return ResponseEntity.ok("Documents are successfully reviewed...");
    }
}
