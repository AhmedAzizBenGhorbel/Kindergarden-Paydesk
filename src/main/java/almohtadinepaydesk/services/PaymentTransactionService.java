package almohtadinepaydesk.services;

import java.sql.Connection;
import java.sql.SQLException;
import almohtadinepaydesk.dao.PaymentEntryDao;
import almohtadinepaydesk.dao.ReceiptDao;
import almohtadinepaydesk.database.DatabaseConnection;
import almohtadinepaydesk.database.DatabaseDiagnostics;
import almohtadinepaydesk.models.PaymentEntry;
import almohtadinepaydesk.models.Receipt;

/** Payment and receipt use one connection and either both commit or both roll back. */
public class PaymentTransactionService {
    @FunctionalInterface public interface ConnectionFactory { Connection open() throws SQLException; }
    private final ConnectionFactory connections;
    private final PaymentEntryDao payments;
    private final ReceiptDao receipts;
    private String lastErrorMessage = "";
    public PaymentTransactionService() { this(DatabaseConnection::getConnection, new PaymentEntryDao(), new ReceiptDao()); }
    public PaymentTransactionService(ConnectionFactory connections, PaymentEntryDao payments, ReceiptDao receipts) {
        this.connections = connections; this.payments = payments; this.receipts = receipts;
    }
    public boolean save(PaymentEntry payment, Receipt receipt) {
        lastErrorMessage = "";
        PaymentCalculationService calculation = new PaymentCalculationService();
        if (payment == null || receipt == null || !calculation.isPositive(payment.getAmount())
                || payment.getPaymentDate() == null || payment.getPaymentMethod() == null) {
            lastErrorMessage = "Montant positif, date et méthode de paiement obligatoires.";
            return false;
        }
        payment.setAmount(calculation.cleanAmount(payment.getAmount()));
        receipt.setTotalAmount(payment.getAmount());
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                if (!payments.createPaymentEntry(connection, payment)) {
                    lastErrorMessage = payments.getLastErrorMessage();
                    connection.rollback();
                    return false;
                }
                receipt.setPaymentEntryId(payment.getId());
                if (!receipts.createReceipt(connection, receipt)) {
                    lastErrorMessage = receipts.getLastErrorMessage();
                    connection.rollback();
                    return false;
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                try { connection.rollback(); }
                catch (SQLException rollback) { DatabaseDiagnostics.report("payment rollback", rollback); }
                throw e;
            }
        } catch (SQLException e) {
            DatabaseDiagnostics.report("payment transaction", e);
            lastErrorMessage = DatabaseDiagnostics.userMessage(e);
            return false;
        }
    }
    public String getLastErrorMessage() { return lastErrorMessage; }
}
