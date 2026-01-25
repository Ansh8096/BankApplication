package net.engineerAnsh.BankApplication.Util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TransactionReferenceGenerator {

    private static final String PREFIX = "TXN";

    public static String generate() {
        return PREFIX + "-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-"
                + UUID.randomUUID().toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
