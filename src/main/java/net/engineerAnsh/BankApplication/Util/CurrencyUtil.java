package net.engineerAnsh.BankApplication.Util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtil {

    private static final Locale INDIA = new Locale("en","IN");

    public static String format(BigDecimal amount){
        NumberFormat formatter = NumberFormat.getCurrencyInstance(INDIA);
        return formatter.format(amount);
    }
}
