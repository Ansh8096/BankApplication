package net.engineerAnsh.BankApplication.Email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Email.event.TransactionEmailRequest;
import net.engineerAnsh.BankApplication.Kafka.Event.*;
import net.engineerAnsh.BankApplication.Utils.MaskingUtil;
import net.engineerAnsh.BankApplication.Utils.CurrencyUtil;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.base-url}")
    private String baseUrl;

    private String formatDate(LocalDateTime time) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        return time.format(formatter);
    }

    private String formatUserName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    private String processTemplateForAccountEvents(String templateName, AccountNotificationEvent event) {
        Context context = new Context();
        context.setVariable("accountNumber", MaskingUtil.maskAccountNumber(event.getAccountNumber()));
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
        context.setVariable("accountNumber", req.getAccountNumber());
        context.setVariable("date", req.getDate());
        context.setVariable("txnId", req.getTxnId());
        context.setVariable("remark", req.getRemark());
        return templateEngine.process(templateName, context);
    }

    private String processTemplateForKyc(KycEvent event, String templateName) {
        Context context = new Context();
        context.setVariable("name", formatUserName(event.getName()));
        context.setVariable("email", MaskingUtil.maskEmail(event.getEmail()));
        context.setVariable("referenceId", event.getReferenceId());
        context.setVariable("documentType", event.getDocumentType());
        context.setVariable("reason", event.getReason());
        context.setVariable("time", formatDate(event.getOccurredAt()));
        String trackLink = baseUrl + "/api/v1/kyc/status/view/" + event.getReferenceId();
        context.setVariable("trackLink", trackLink);
        return templateEngine.process(templateName, context);
    }

    private String processTemplateForFrauds(FraudDetectedEvent event, String templateName) {
        Context context = new Context();
        context.setVariable("name", formatUserName(event.getName()));
        context.setVariable("accountNumber", event.getAccountNumber());
        context.setVariable("reason", event.getReason());
        context.setVariable("amount", CurrencyUtil.format(event.getAmount()).replaceAll("\\.00$", ""));
        context.setVariable("transactionType", event.getTransactionType());
        context.setVariable("transactionReference", event.getTransactionReference());
        context.setVariable("time", formatDate(event.getOccurredAt()));
        return templateEngine.process(templateName, context);
    }

    public String buildAccountEmailBody(AccountNotificationEvent event) {
        return switch (event.getEventType()) {
            case ACCOUNT_CREATED -> processTemplateForAccountEvents("email/account/created", event);
            case ACCOUNT_ACTIVATED -> processTemplateForAccountEvents("email/account/activated", event);
            case ACCOUNT_BLOCKED -> processTemplateForAccountEvents("email/account/blocked", event);
            case ACCOUNT_FROZEN -> processTemplateForAccountEvents("email/account/frozen", event);
            case ACCOUNT_CLOSED -> processTemplateForAccountEvents("email/account/closed", event);
        };
    }

    public String buildTxnEmailBody(TransactionCompletedEvent event) {
        TransactionEmailRequest req = mapToRequest(event);
        return switch (event.getType()) {
            case "TRANSFER_SENT", "WITHDRAW" -> processTemplateForTxn("email/transaction/debit", req);
            case "TRANSFER_RECEIVED", "DEPOSIT" -> processTemplateForTxn("email/transaction/credit", req);
            default -> "";
        };
    }

    public String buildVerificationEmail(String verificationLink) {
        Context context = new Context();
        context.setVariable("verificationLink", verificationLink);
        return templateEngine.process("email/auth/verify", context);
    }

    public String buildLoginAlertEmail(UserLoginEvent loginEvent) {
        Context context = new Context();
        context.setVariable("name", formatUserName(loginEvent.getUserName()));
        context.setVariable("ip", loginEvent.getIpAddress());
        context.setVariable("location", loginEvent.getLocation());
        context.setVariable("device", loginEvent.getUserAgent());
        context.setVariable("time", formatDate(loginEvent.getOccurredAt()));
        return templateEngine.process("email/auth/login-alert", context);
    }

    public String buildKycAlertEmail(KycEvent event) {
        return switch (event.getEventType()) {
            case SUBMITTED -> processTemplateForKyc(event, "email/kyc/submitted-user");
            case APPROVED -> processTemplateForKyc(event, "email/kyc/approved");
            case REJECTED -> processTemplateForKyc(event, "email/kyc/rejected");
        };
    }

    public String buildKycAlertEmailForAdmin(KycEvent event) {
        return processTemplateForKyc(event, "email/kyc/submitted-admin");
    }

    public String buildFraudDetectedEmailBody(FraudDetectedEvent event) {
        return switch (event.getDecision()) {
            case FREEZE_ACCOUNT -> processTemplateForFrauds(event, "email/fraud/frozen");
            case BLOCK -> processTemplateForFrauds(event, "email/fraud/blocked");
            case SUSPICIOUS -> processTemplateForFrauds(event, "email/fraud/suspicious");
            case SAFE -> processTemplateForFrauds(event, "email/fraud/safe");
        };
    }

}