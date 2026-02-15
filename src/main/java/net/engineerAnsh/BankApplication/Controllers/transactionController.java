package net.engineerAnsh.BankApplication.Controllers;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.transaction.DepositRequest;
import net.engineerAnsh.BankApplication.Dto.Statements.AccountStatementDto;
import net.engineerAnsh.BankApplication.Dto.transaction.TransactionResponse;
import net.engineerAnsh.BankApplication.Dto.transaction.TransferRequest;
import net.engineerAnsh.BankApplication.Dto.transaction.WithdrawRequest;
import net.engineerAnsh.BankApplication.Services.StatementPdfService;
import net.engineerAnsh.BankApplication.Services.TransactionService;
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
public class transactionController {

    private final TransactionService transactionService;
    private final StatementPdfService statementPdfService;


    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody TransferRequest request) throws AccessDeniedException {
        TransactionResponse transactionResponse = transactionService.transferMoneyBetweenAccounts(request);
        return ResponseEntity.ok().body(transactionResponse);
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> depositMoney(@RequestBody DepositRequest request) throws AccessDeniedException {
        TransactionResponse transactionResponse = transactionService.depositMoneyToTheAccount(request);
        return ResponseEntity.ok().body(transactionResponse);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdrawMoney(@RequestBody WithdrawRequest request) throws AccessDeniedException {
        TransactionResponse transactionResponse = transactionService.withdrawMoneyFromTheAccount(request);
        return ResponseEntity.ok().body(transactionResponse);
    }

    @GetMapping("/statement/pdf/{accountNumber}")
    public ResponseEntity<byte[]> getStatement(
            @PathVariable String accountNumber,

            @RequestParam(name = "from") // variableName...
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(name = "to") // variableName...
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
