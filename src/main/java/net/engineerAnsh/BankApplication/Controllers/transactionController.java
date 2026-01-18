package net.engineerAnsh.BankApplication.Controllers;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.DepositRequest;
import net.engineerAnsh.BankApplication.Dto.TransferRequest;
import net.engineerAnsh.BankApplication.Dto.WithdrawRequest;
import net.engineerAnsh.BankApplication.Entity.Transaction;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Services.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class transactionController {

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;


    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody TransferRequest request) throws AccessDeniedException {
        transactionService.transferMoneyBetweenAccounts(request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount(),
                request.getRemark()
        );
        return ResponseEntity.ok().body("Transfer is successful");
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> depositMoney(@RequestBody DepositRequest request) throws AccessDeniedException {
        transactionService.depositMoneyToTheAccount(request.getAccountNo(),
                request.getAmount(),
                request.getRemark()
        );
        return ResponseEntity.ok().body("Amount is successfully deposited");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdrawMoney(@RequestBody WithdrawRequest request) throws AccessDeniedException {
        transactionService.withdrawMoneyFromTheAccount(request.getAccountNo(),
                request.getAmount(),
                request.getRemark()
        );
        return ResponseEntity.ok().body("withdraw successful");
    }

    @GetMapping("/statement/{accountNo}")
    public ResponseEntity<?> getStatement(@PathVariable String accountNo) throws AccessDeniedException {
        List<Transaction> transactionsHistoryOfTheAccount = transactionService.getTransactionsHistoryOfTheAccount(accountNo);
        return ResponseEntity.ok().body(transactionsHistoryOfTheAccount);
    }

}
