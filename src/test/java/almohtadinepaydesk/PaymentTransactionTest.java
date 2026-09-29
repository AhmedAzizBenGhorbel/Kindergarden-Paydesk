package almohtadinepaydesk;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import almohtadinepaydesk.dao.*;
import almohtadinepaydesk.models.*;
import almohtadinepaydesk.services.*;

class PaymentTransactionTest {
    private String database() throws SQLException {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE payment_entries (id INT AUTO_INCREMENT PRIMARY KEY, monthly_record_id INT, extra_id INT, amount DECIMAL(10,3), payment_date DATE, payment_method VARCHAR(30), notes VARCHAR(200), created_by_user_id INT, active BOOLEAN)");
            s.execute("CREATE TABLE receipts (id INT AUTO_INCREMENT PRIMARY KEY, receipt_number VARCHAR(50) UNIQUE NOT NULL, child_id INT, payment_entry_id INT REFERENCES payment_entries(id), receipt_date DATE, total_amount DECIMAL(10,3), payer_name VARCHAR(100), notes VARCHAR(200), active BOOLEAN)");
        }
        return url;
    }
    private PaymentEntry payment(String amount) {
        PaymentEntry p = new PaymentEntry(); p.setMonthlyRecordId(1); p.setAmount(new BigDecimal(amount));
        p.setPaymentDate(LocalDate.of(2026,9,29)); p.setPaymentMethod(PaymentMethod.ESPECES); return p;
    }
    private Receipt receipt(String number) { return new Receipt(number, 1, LocalDate.of(2026,9,29), BigDecimal.ZERO); }
    private int count(String url, String table) throws SQLException {
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) { r.next(); return r.getInt(1); }
    }
    @Test void commitsLinkedPaymentAndReceipt() throws Exception {
        String url = database(); var service = new PaymentTransactionService(() -> DriverManager.getConnection(url), new PaymentEntryDao(), new ReceiptDao());
        PaymentEntry p = payment("35.500"); Receipt r = receipt("LAB-1");
        assertTrue(service.save(p,r)); assertEquals(p.getId(), r.getPaymentEntryId());
        assertEquals(new BigDecimal("35.500"),r.getTotalAmount());
        assertEquals(1,count(url,"payment_entries")); assertEquals(1,count(url,"receipts"));
    }
    @Test void duplicateReceiptRollsBackSecondPayment() throws Exception {
        String url = database(); var service = new PaymentTransactionService(() -> DriverManager.getConnection(url), new PaymentEntryDao(), new ReceiptDao());
        assertTrue(service.save(payment("20"),receipt("LAB-DUP")));
        assertFalse(service.save(payment("10"),receipt("LAB-DUP")));
        assertEquals(1,count(url,"payment_entries")); assertEquals(1,count(url,"receipts"));
    }
    @Test void invalidAmountsNeverOpenConnection() {
        var service = new PaymentTransactionService(() -> { fail("Invalid payment opened a connection"); return null; },new PaymentEntryDao(),new ReceiptDao());
        for (String amount : new String[]{"0", "-1", "0.0001"}) assertFalse(service.save(payment(amount),receipt("INVALID")));
        PaymentEntry p = payment("5"); p.setAmount(null); assertFalse(service.save(p,receipt("INVALID")));
    }
    @Test void balancesAndPartialStatus() {
        var calculations = new PaymentCalculationService(); var statuses = new PaymentStatusService();
        BigDecimal expected = calculations.calculateTotalExpected(new BigDecimal("100"),new BigDecimal("15"));
        assertEquals(new BigDecimal("75.000"), calculations.calculateRemainingAmount(expected,new BigDecimal("40")));
        assertEquals(new BigDecimal("0.000"),calculations.calculateAdvanceAmount(expected,new BigDecimal("40")));
        assertEquals(new BigDecimal("0.000"),calculations.calculateRemainingAmount(expected,new BigDecimal("125")));
        assertEquals(new BigDecimal("10.000"),calculations.calculateAdvanceAmount(expected,new BigDecimal("125")));
        assertEquals(PaymentStatus.PARTIEL,statuses.calculateStatus(new BigDecimal("40"),expected,LocalDate.now().plusDays(1)));
        assertEquals(PaymentStatus.EN_RETARD,statuses.calculateStatus(new BigDecimal("40"),expected,LocalDate.now().minusDays(1)));
        assertEquals(PaymentStatus.PAYE,statuses.calculateStatus(expected,expected,LocalDate.now()));
    }
}
