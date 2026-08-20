package almohtadinepaydesk.services;

import java.math.BigDecimal;
import java.time.LocalDate;

import almohtadinepaydesk.models.PaymentStatus;

public class PaymentStatusService {

    public PaymentStatus calculateStatus(BigDecimal totalPaid, BigDecimal totalExpected, LocalDate deadlineDate) {
        BigDecimal paid = normalize(totalPaid);
        BigDecimal expected = normalize(totalExpected);
        LocalDate today = LocalDate.now();

        if (deadlineDate == null) {
            deadlineDate = today;
        }

        if (paid.compareTo(expected) >= 0) {
            return PaymentStatus.PAYE;
        }

        if (paid.compareTo(BigDecimal.ZERO) == 0 && !today.isAfter(deadlineDate)) {
            return PaymentStatus.NON_PAYE;
        }

        if (paid.compareTo(BigDecimal.ZERO) == 0 && today.isAfter(deadlineDate)) {
            return PaymentStatus.EN_RETARD;
        }

        if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(expected) < 0 && !today.isAfter(deadlineDate)) {
            return PaymentStatus.PARTIEL;
        }

        return PaymentStatus.EN_RETARD;
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        return amount;
    }
}
