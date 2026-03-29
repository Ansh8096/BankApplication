package net.engineerAnsh.BankApplication.Email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.event.TransactionEmailRequest;
import net.engineerAnsh.BankApplication.Kafka.Event.AccountNotificationEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.FraudDetectedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.UserLoginEvent;
import net.engineerAnsh.BankApplication.Utils.AccountMaskingUtil;
import net.engineerAnsh.BankApplication.Utils.CurrencyUtil;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final SpringTemplateEngine templateEngine;

    private String formatDate(LocalDateTime time) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return time.format(formatter);
    }

    private String formatUserName(String name){
        return name.substring(0,1).toUpperCase() + name.substring(1).toLowerCase();
    }

    private String processTemplateForAccountEvents(String templateName, AccountNotificationEvent event) {
        Context context = new Context();
        context.setVariable("accountNumber", AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()));
        context.setVariable("accountType", event.getAccountType());
        context.setVariable("formattedDate", formatDate(event.getTimestamp()));
        return templateEngine.process(templateName, context);
    }

    private TransactionEmailRequest mapToRequest(TransactionCompletedEvent event) {
        return new TransactionEmailRequest(
                event.getAmount(),
                event.getFromAccountMasked() != null
                        ? event.getFromAccountMasked()
                        : event.getToAccountMasked(),
                formatDate(event.getCreatedAt()),
                event.getTransactionReference(),
                event.getRemark()
        );
    }

    private String processTemplateForTxn(String templateName, TransactionEmailRequest req) {
        Context context = new Context();
        context.setVariable("amount", CurrencyUtil.format(req.getAmount()).replaceAll("\\.00$", ""));
        context.setVariable("accountNumber",req.getAccountNumber());
        context.setVariable("date", req.getDate());
        context.setVariable("txnId", req.getTxnId());
        context.setVariable("remark", req.getRemark());
        return templateEngine.process(templateName, context);
    }

    public String buildAccountEmailBody(AccountNotificationEvent event) {
        return switch (event.getEventType()) {
            case ACCOUNT_CREATED -> processTemplateForAccountEvents("email/account-created", event);
            case ACCOUNT_ACTIVATED -> processTemplateForAccountEvents("email/account-activated", event);
            case ACCOUNT_BLOCKED -> processTemplateForAccountEvents("email/account-blocked", event);
            case ACCOUNT_FROZEN -> processTemplateForAccountEvents("email/account-frozen", event);
            case ACCOUNT_CLOSED -> processTemplateForAccountEvents("email/account-closed", event);
        };
    }

    public String buildTxnEmailBody(TransactionCompletedEvent event) {
        TransactionEmailRequest req = mapToRequest(event);
        return switch (event.getType()) {
            case "TRANSFER_SENT", "WITHDRAW" -> processTemplateForTxn("email/debit",req);
            case "TRANSFER_RECEIVED", "DEPOSIT" -> processTemplateForTxn("email/credit",req);
            default -> "";
        };
    }

    public String buildVerificationEmail(String verificationLink) {
        Context context = new Context();
        context.setVariable("verificationLink", verificationLink);
        return templateEngine.process("email/verify", context);
    }

    public String buildLoginAlertEmail(UserLoginEvent loginEvent, String secureLink) {
        Context context = new Context();
        context.setVariable("name", formatUserName(loginEvent.getUserName()));
        context.setVariable("ip", loginEvent.getIpAddress());
        context.setVariable("device", loginEvent.getUserAgent());
        context.setVariable("time", formatDate(loginEvent.getOccurredAt()));
        context.setVariable("secureLink", secureLink);
        return templateEngine.process("email/login-alert", context);
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

}