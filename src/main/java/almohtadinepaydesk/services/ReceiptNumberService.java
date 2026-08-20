package almohtadinepaydesk.services;

import java.time.LocalDate;

import almohtadinepaydesk.dao.ReceiptDao;

public class ReceiptNumberService {

    private final ReceiptDao receiptDao = new ReceiptDao();

    public String generateReceiptNumber(LocalDate receiptDate) {
        int year = receiptDate.getYear();
        String lastReceiptNumber = receiptDao.findLastReceiptNumberForYear(year);
        int nextNumber = 1;

        if (lastReceiptNumber != null && lastReceiptNumber.length() >= 13) {
            String numberPart = lastReceiptNumber.substring(lastReceiptNumber.lastIndexOf("-") + 1);

            try {
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException e) {
                nextNumber = 1;
            }
        }

        return "ADP-" + year + "-" + String.format("%04d", nextNumber);
    }
}
