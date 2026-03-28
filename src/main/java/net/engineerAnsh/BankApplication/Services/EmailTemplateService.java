package net.engineerAnsh.BankApplication.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Kafka.Event.AccountNotificationEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.FraudDetectedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
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

    private String processTemplate(String templateName, AccountNotificationEvent event) {
        Context context = new Context();
        context.setVariable("accountNumber", AccountMaskingUtil.maskAccountNumber(event.getAccountNumber()));
        context.setVariable("accountType", event.getAccountType());
        context.setVariable("formattedDate", formatDate(event.getTimestamp()));
        return templateEngine.process(templateName, context);
    }

    public String buildAccountEmailBody(AccountNotificationEvent event) {
        return switch (event.getEventType()) {
            case ACCOUNT_CREATED -> processTemplate("email/account-created", event);
            case ACCOUNT_ACTIVATED -> processTemplate("email/account-activated", event);
            case ACCOUNT_BLOCKED -> processTemplate("email/account-blocked", event);
            case ACCOUNT_FROZEN -> processTemplate("email/account-frozen", event);
            case ACCOUNT_CLOSED -> processTemplate("email/account-closed", event);
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