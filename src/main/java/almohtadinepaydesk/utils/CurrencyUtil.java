package almohtadinepaydesk.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyUtil {

    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        BigDecimal scaledAmount = amount.setScale(3, RoundingMode.HALF_UP);

        if (scaledAmount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return scaledAmount.setScale(0, RoundingMode.HALF_UP).toPlainString() + " TND";
        }

        return scaledAmount.toPlainString() + " TND";
    }

    private CurrencyUtil() {
    }
}
