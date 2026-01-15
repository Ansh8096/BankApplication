package net.engineerAnsh.BankApplication.Controllers;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Dto.TransactionRequest;
import net.engineerAnsh.BankApplication.Entity.Account;
import net.engineerAnsh.BankApplication.Repository.AccountRepository;
import net.engineerAnsh.BankApplication.Services.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.AccessDeniedException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class transactionController {

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;


    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(TransactionRequest request) throws AccessDeniedException {

        Account fromAccount = accountRepository.findById(request.getFromAccountId()).orElseThrow(()-> new UsernameNotFoundException("The user account is not found"));
        Account toAccount = accountRepository.findById(request.getToAccountId()).orElseThrow(()-> new UsernameNotFoundException("The receiver account is not found"));

        transactionService.transferMoney(fromAccount,
                toAccount,
                request.getAmount(),
                request.getRemark()
        );

        return ResponseEntity.ok().body("Transfer is successful");
    }

}
