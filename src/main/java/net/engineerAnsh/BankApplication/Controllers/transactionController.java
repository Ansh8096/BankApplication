package net.engineerAnsh.BankApplication.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.transaction.DepositRequest;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Dto.transaction.TransactionResponse;
import net.engineerAnsh.BankApplication.Dto.transaction.TransferRequest;
import net.engineerAnsh.BankApplication.Dto.transaction.WithdrawRequest;
import net.engineerAnsh.BankApplication.services.statement.StatementPdfService;
import net.engineerAnsh.BankApplication.services.transaction.TransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transaction")
@Tag(name = "Transaction APIs", description = "Operations related to money transfer, deposit, withdrawal and statements")
public class transactionController {

    private final TransactionService transactionService;
    private final StatementPdfService statementPdfService;


    @Operation(summary = "Transfer money", description = "Transfer money between two accounts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer successful"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(
            @Parameter(description = "Transfer request payload", required = true)
            @RequestBody TransferRequest request) throws AccessDeniedException, JsonProcessingException {

        TransactionResponse transactionResponse = transactionService.transferMoneyBetweenAccounts(request);
        return ResponseEntity.ok().body(transactionResponse);
    }


    @Operation(summary = "Deposit money", description = "Deposit money into an account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit successful"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/deposit")
    public ResponseEntity<?> depositMoney(
            @Parameter(description = "Deposit request payload", required = true)
            @RequestBody DepositRequest request) throws AccessDeniedException, JsonProcessingException {

        TransactionResponse transactionResponse = transactionService.depositMoneyToTheAccount(request);
        return ResponseEntity.ok().body(transactionResponse);
    }


    @Operation(summary = "Withdraw money", description = "Withdraw money from an account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawal successful"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdrawMoney(
            @Parameter(description = "Withdraw request payload", required = true)
            @RequestBody WithdrawRequest request) throws AccessDeniedException, JsonProcessingException {

        TransactionResponse transactionResponse = transactionService.withdrawMoneyFromTheAccount(request);
        return ResponseEntity.ok().body(transactionResponse);
    }



    @Operation(summary = "Download account statement", description = "Generate and download account statement PDF")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/statement/pdf/{accountNumber}")
    public ResponseEntity<byte[]> getStatement(

            @Parameter(description = "Account number", example = "1234567890")
            @PathVariable String accountNumber,

            @Parameter(description = "Start date (yyyy-MM-dd)", example = "2026-01-01")
            @RequestParam(name = "from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @Parameter(description = "End date (yyyy-MM-dd)", example = "2026-01-31")
            @RequestParam(name = "to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to

    ) throws AccessDeniedException {

        // Getting the accountStatement in the form of Dto...
        AccountStatementDto generatedStatement = transactionService.generateStatement(accountNumber, from, to);

        // Converting this accountStatement in the proper format pdf...
        byte[] pdf = statementPdfService.generatePdf(generatedStatement);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=statement.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

}
