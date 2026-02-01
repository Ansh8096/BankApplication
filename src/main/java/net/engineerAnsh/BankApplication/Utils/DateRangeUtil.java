package net.engineerAnsh.BankApplication.Utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateRangeUtil {

    // We always calculate date boundaries in IST:
    public static LocalDateTime startOfTodayIST() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata")).atStartOfDay();
    }

    public static LocalDateTime startOfTomorrowIST() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).atStartOfDay();
    }

}
