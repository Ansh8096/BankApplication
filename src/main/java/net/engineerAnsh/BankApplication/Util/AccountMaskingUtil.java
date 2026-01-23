package net.engineerAnsh.BankApplication.Util;

public class AccountMaskingUtil {

    public static String maskAccountNumber(String accountNo){
        if(accountNo == null || accountNo.length() <= 6){
            return "****";
        }

        String prefix = accountNo.substring(0,6);
        String last4Digits = accountNo.substring(accountNo.length() - 4);

        return (prefix + "****" + last4Digits);
    }
}
