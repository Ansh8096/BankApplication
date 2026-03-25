package net.engineerAnsh.BankApplication.Services;

import net.engineerAnsh.BankApplication.Kafka.Event.AccountNotificationEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.FraudDetectedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Utils.AccountMaskingUtil;
import net.engineerAnsh.BankApplication.Utils.CurrencyUtil;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String buildAccountEmailBody(AccountNotificationEvent event) {
        return switch (event.getEventType()) {
            case ACCOUNT_CREATED -> String.format("""
                            Dear Customer,
                            
                            Your %s account has been successfully created with our bank.
                            
                            Account Details:
                            Account Number: %s
                            Account Type: %s
                            Created On: %s
                            
                            You can now log in to your account and start using our banking services.
                            
                            If you did not initiate this request, please contact our support team immediately.
                            
                            Thank you for banking with us.
                            
                            Regards,
                            Bank Support Team
                            """,
                    event.getAccountType(),
                    AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()),
                    event.getAccountType(),
                    event.getTimestamp()
            );

            case ACCOUNT_ACTIVATED -> String.format("""
                            Dear Customer,
                            
                            Your bank account has been successfully activated.
                            
                            Account Details:
                            Account Number: %s
                            Account Type: %s
                            Activated On: %s
                            
                            You can now perform transactions such as deposits, withdrawals, and transfers.
                            
                            If you notice any suspicious activity, please contact our support immediately.
                            
                            Regards,
                            Bank Support Team
                            """,
                    AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()),
                    event.getAccountType(),
                    event.getTimestamp());

            case ACCOUNT_BLOCKED -> String.format("""
                            Dear Customer,
                            
                            Your bank account has been temporarily blocked for security reasons.
                            
                            Account Details:
                            Account Number: %s
                            Account Type: %s
                            Blocked On: %s
                            
                            Possible reasons may include suspicious activity or administrative review.
                            
                            Please contact customer support to restore access to your account.
                            
                            Regards,
                            Bank Security Team
                            """,
                    AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()),
                    event.getAccountType(),
                    event.getTimestamp());

            case ACCOUNT_FROZEN -> String.format("""
                            Dear Customer,
                            
                            Your account has been temporarily frozen.
                            
                            Account Details:
                            Account Number: %s
                            Account Type: %s
                            Frozen On: %s
                            
                            While your account is frozen, transactions such as withdrawals or transfers will not be permitted.
                            
                            Please contact customer support for further assistance.
                            
                            Regards,
                            Bank Support Team
                            """,
                    AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()),
                    event.getAccountType(),
                    event.getTimestamp());

            case ACCOUNT_CLOSED -> String.format("""
                            Dear Customer,
                            
                            Your bank account has been permanently closed.
                            
                            Account Details:
                            Account Number: %s
                            Account Type: %s
                            Closed On: %s
                            
                            If this closure was not initiated by you, please contact customer support immediately.
                            
                            We appreciate the time you spent banking with us.
                            
                            Regards,
                            Bank Support Team
                            """,
                    AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()),
                    event.getAccountType(),
                    event.getTimestamp());
        };
    }

    public String buildFraudDetectedEmailBody(FraudDetectedEvent event) {
        return switch (event.getDecision()) {
            case FREEZE_ACCOUNT -> String.format("""
                            Dear Customer,
                            
                            Your account (%s) has been temporarily frozen due to suspicious activity.
                            
                            Reason: %s
                            
                            Please contact support immediately to restore access.
                            
                            - Bank of Ansh
                            """,
                    AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()),
                    event.getReason()
            );

            case BLOCK -> String.format("""
                            Dear Customer,
                            
                            A transaction of %s was blocked for your account (%s).
                            
                            Reason: %s
                            
                            If this wasn't you, please contact support.
                            
                            - Bank of Ansh
                            """,
                    CurrencyUtil.format(event.getAmount()),
                    AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()),
                    event.getReason()
            );

            case SUSPICIOUS -> String.format("""
                            Dear Customer,
                            
                            We detected unusual activity on your account (%s).
                            
                            Transaction Amount: %s
                            
                            If this was not you, please take action immediately.
                            
                            - Bank of Ansh
                            """,
                    AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()),
                    CurrencyUtil.format(event.getAmount())
            );

            default -> "";
        };
    }

    public String buildTxnEmailBody(TransactionCompletedEvent event) {
        return String.format(
                """
                        Hello,
                        
                        A transaction has been completed on your account.
                        
                        Reference: %s
                        Type: %s
                        Amount: ₹%s
                        From: %s
                        To: %s
                        Time: %s
                        Remark: %s
                        
                        Thank you,
                        BANK OF ANSH
                        """.formatted(
                        event.getTransactionReference(),
                        event.getType(),
                        event.getAmount(),
                        event.getFromAccountMasked(),
                        event.getToAccountMasked(),
                        event.getCreatedAt(),
                        event.getRemark() != null ? event.getRemark() : "-"
                ));
    }

}