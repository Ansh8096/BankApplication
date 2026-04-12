package net.engineerAnsh.BankApplication.Utils;

public class MaskingUtil {

    public static String maskAccountNumber(String accountNo){
        if(accountNo == null || accountNo.length() <= 6){
            return "****";
        }

        String prefix = accountNo.substring(0,6);
        String last4Digits = accountNo.substring(accountNo.length() - 4);

        return (prefix + "****" + last4Digits);
    }

    public static String maskEmail(String email) {
        int at = email.indexOf("@");
        if (at <= 2) return email;

        return email.substring(0, 2) + "****" + email.substring(at);
    }

    public static String maskPhone(String phone) {

        if (phone == null || phone.length() < 6) {
            return "******";
        }

        // Handle +91 format
        if (phone.startsWith("+")) {

            String countryCode = phone.substring(0, phone.length() - 10);
            String number = phone.substring(phone.length() - 10);

            return countryCode + " " +
                    number.substring(0, 2) +
                    "******" +
                    number.substring(8);
        }

        // Normal 10-digit number
        return phone.substring(0, 2) +
                "******" +
                phone.substring(phone.length() - 2);
    }

}
