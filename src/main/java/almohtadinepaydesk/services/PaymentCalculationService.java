package almohtadinepaydesk.services;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentCalculationService {

    public BigDecimal calculateTotalExpected(BigDecimal baseMonthlyFee, BigDecimal totalExtras) {
        return cleanAmount(baseMonthlyFee).add(cleanAmount(totalExtras));
    }

    public BigDecimal calculateRemainingAmount(BigDecimal totalExpected, BigDecimal totalPaid) {
        BigDecimal remaining = cleanAmount(totalExpected).subtract(cleanAmount(totalPaid));

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }

        return cleanAmount(remaining);
    }

    public BigDecimal calculateAdvanceAmount(BigDecimal totalExpected, BigDecimal totalPaid) {
        BigDecimal advance = cleanAmount(totalPaid).subtract(cleanAmount(totalExpected));

        if (advance.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }

        return cleanAmount(advance);
    }

    public BigDecimal cleanAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }

        return amount.setScale(3, RoundingMode.HALF_UP);
    }

    public boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
