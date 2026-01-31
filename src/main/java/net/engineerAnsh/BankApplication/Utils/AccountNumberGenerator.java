package net.engineerAnsh.BankApplication.Utils;

// Why util package?
//  Because:
//  It has no DB access
//  No Spring annotations
//  Stateless
//  Reusable logic
//  Pure Java logic

import net.engineerAnsh.BankApplication.Entity.Account;

// This is the Key Idea:--
// AccountNumber = bankCode + ACCOUNT_TYPE + PADDED_ID
// eg: BNK + SAV + 000000123 → BNKSAV000000123

public class AccountNumberGenerator {

    private static final String bankCode = "BNK";

    public static String generateAccountNumber(Account account){
        return bankCode +
                account.getAccountType().name().substring(0,3) +
                String.format("%09d", account.getId());
    }

}

