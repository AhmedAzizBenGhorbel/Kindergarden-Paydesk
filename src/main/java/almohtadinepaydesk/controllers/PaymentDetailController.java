package almohtadinepaydesk.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;

import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.dao.MonthlyRecordDao;
import almohtadinepaydesk.dao.PaymentEntryDao;
import almohtadinepaydesk.models.MonthlyRecord;
import almohtadinepaydesk.models.PaymentEntry;
import almohtadinepaydesk.models.PaymentMethod;
import almohtadinepaydesk.models.Receipt;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.services.PaymentCalculationService;
import almohtadinepaydesk.services.ReceiptNumberService;
import almohtadinepaydesk.services.PaymentTransactionService;
import almohtadinepaydesk.utils.AlertUtil;
import almohtadinepaydesk.utils.CurrencyUtil;
import almohtadinepaydesk.utils.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PaymentDetailController {

    private final MonthlyRecordDao monthlyRecordDao = new MonthlyRecordDao();
    private final PaymentEntryDao paymentEntryDao = new PaymentEntryDao();
    private final PaymentTransactionService transactionService = new PaymentTransactionService();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final PaymentCalculationService calculationService = new PaymentCalculationService();
    private final ReceiptNumberService receiptNumberService = new ReceiptNumberService();
    private final ObservableList<PaymentEntry> paymentEntries = FXCollections.observableArrayList();

    private int monthlyRecordId;
    private MonthlyRecord currentRecord;

    @FXML
    private Label childNameLabel;

    @FXML
    private Label parentNameLabel;

    @FXML
    private Label schoolYearLabel;

    @FXML
    private Label monthLabel;

    @FXML
    private Label baseFeeLabel;

    @FXML
    private Label extrasLabel;

    @FXML
    private Label expectedLabel;

    @FXML
    private Label totalPaidLabel;

    @FXML
    private Label remainingLabel;

    @FXML
    private Label advanceLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private TableView<PaymentEntry> historyTable;

    @FXML
    private TableColumn<PaymentEntry, String> paymentDateColumn;

    @FXML
    private TableColumn<PaymentEntry, String> amountColumn;

    @FXML
    private TableColumn<PaymentEntry, String> methodColumn;

    @FXML
    private TableColumn<PaymentEntry, String> receiptColumn;

    @FXML
    private TableColumn<PaymentEntry, String> noteColumn;

    @FXML
    private TextField amountField;

    @FXML
    private DatePicker paymentDatePicker;

    @FXML
    private ComboBox<PaymentMethod> methodComboBox;

    @FXML
    private TextArea noteArea;

    @FXML
    private void initialize() {
        configureHistoryTable();
        configurePaymentForm();
    }

    public void setMonthlyRecordId(int monthlyRecordId) {
        this.monthlyRecordId = monthlyRecordId;
        loadDetails();
    }

    @FXML
    private void handleSavePayment() {
        BigDecimal amount = parseAmount();
        LocalDate paymentDate = paymentDatePicker.getValue();
        PaymentMethod paymentMethod = methodComboBox.getValue();

        if (!validatePayment(amount, paymentDate, paymentMethod)) {
            return;
        }

        PaymentEntry paymentEntry = new PaymentEntry();
        paymentEntry.setMonthlyRecordId(monthlyRecordId);
        paymentEntry.setAmount(calculationService.cleanAmount(amount));
        paymentEntry.setPaymentDate(paymentDate);
        paymentEntry.setPaymentMethod(paymentMethod);
        paymentEntry.setNotes(emptyToNull(noteArea.getText()));
        paymentEntry.setCreatedByUserId(getCurrentUserId());
        paymentEntry.setActive(true);

        String receiptNumber = receiptNumberService.generateReceiptNumber(paymentDate);
        Receipt receipt = new Receipt(receiptNumber, currentRecord.getChildId(), paymentDate, paymentEntry.getAmount());
        receipt.setPayerName(emptyToNull(currentRecord.getParentFullName()));
        receipt.setNotes(paymentEntry.getNotes());
        receipt.setActive(true);

        if (!savePaymentAndReceipt(paymentEntry, receipt)) {
            return;
        }

        monthlyRecordDao.recalculateAllMonthlyRecords();
        logPaymentAction(paymentEntry, receiptNumber);
        logReceiptAction(receipt);
        AlertUtil.showInfo("Paiement", "Paiement enregistr\u00e9 avec succ\u00e8s. Re\u00e7u: " + receiptNumber);
        resetPaymentForm();
        loadDetails();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) historyTable.getScene().getWindow();
        stage.close();
    }

    private void configureHistoryTable() {
        paymentDateColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(DateUtil.formatDate(cellData.getValue().getPaymentDate())));
        amountColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(CurrencyUtil.formatAmount(cellData.getValue().getAmount())));
        methodColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(formatPaymentMethod(cellData.getValue().getPaymentMethod())));
        receiptColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getReceiptNumber())));
        noteColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getNotes())));

        historyTable.setItems(paymentEntries);
    }

    private void configurePaymentForm() {
        methodComboBox.getItems().setAll(PaymentMethod.values());
        methodComboBox.setValue(PaymentMethod.ESPECES);
        paymentDatePicker.setValue(LocalDate.now());
    }

    private void loadDetails() {
        monthlyRecordDao.recalculateAllMonthlyRecords();
        currentRecord = monthlyRecordDao.findDetailedById(monthlyRecordId);

        if (currentRecord == null) {
            AlertUtil.showError("Paiement", monthlyRecordDao.getLastErrorMessage());
            return;
        }

        showMonthlyRecordDetails();
        paymentEntries.setAll(paymentEntryDao.listByMonthlyRecordId(monthlyRecordId));
    }

    private void showMonthlyRecordDetails() {
        childNameLabel.setText(valueOrEmpty(currentRecord.getChildFullName()));
        parentNameLabel.setText(valueOrEmpty(currentRecord.getParentFullName()));
        schoolYearLabel.setText(valueOrEmpty(currentRecord.getSchoolYearName()));
        monthLabel.setText(valueOrEmpty(currentRecord.getMonthName()));
        baseFeeLabel.setText(CurrencyUtil.formatAmount(currentRecord.getBaseMonthlyFee()));
        extrasLabel.setText(CurrencyUtil.formatAmount(currentRecord.getTotalExtras()));
        expectedLabel.setText(CurrencyUtil.formatAmount(currentRecord.getTotalExpected()));
        totalPaidLabel.setText(CurrencyUtil.formatAmount(currentRecord.getTotalPaid()));
        remainingLabel.setText(CurrencyUtil.formatAmount(currentRecord.getRemainingAmount()));
        advanceLabel.setText(formatAdvance(currentRecord.getAdvanceAmount()));
        statusLabel.setText(currentRecord.getPaymentStatus().getLabel());
    }

    private boolean validatePayment(BigDecimal amount, LocalDate paymentDate, PaymentMethod paymentMethod) {
        if (currentRecord == null) {
            AlertUtil.showWarning("Paiement", "Aucun paiement mensuel n'est s\u00e9lectionn\u00e9.");
            return false;
        }

        if (!calculationService.isPositive(amount)) {
            AlertUtil.showWarning("Paiement", "Le montant pay\u00e9 doit \u00eatre positif.");
            return false;
        }

        if (paymentDate == null) {
            AlertUtil.showWarning("Paiement", "La date de paiement est obligatoire.");
            return false;
        }

        if (paymentMethod == null) {
            AlertUtil.showWarning("Paiement", "La m\u00e9thode de paiement est obligatoire.");
            return false;
        }

        return true;
    }

    private boolean savePaymentAndReceipt(PaymentEntry paymentEntry, Receipt receipt) {
        if (transactionService.save(paymentEntry, receipt)) return true;
        AlertUtil.showError("Paiement", transactionService.getLastErrorMessage());
        return false;
    }

    private BigDecimal parseAmount() {
        String text = amountField.getText();

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(text.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void resetPaymentForm() {
        amountField.clear();
        paymentDatePicker.setValue(LocalDate.now());
        methodComboBox.setValue(PaymentMethod.ESPECES);
        noteArea.clear();
    }

    private String formatAdvance(BigDecimal advanceAmount) {
        if (advanceAmount != null && advanceAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "Avance: " + CurrencyUtil.formatAmount(advanceAmount);
        }

        return CurrencyUtil.formatAmount(BigDecimal.ZERO);
    }

    private String formatPaymentMethod(PaymentMethod method) {
        if (method == null) {
            return "";
        }

        return method.getLabel();
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private Integer getCurrentUserId() {
        User currentUser = Session.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return currentUser.getId();
    }

    private void logPaymentAction(PaymentEntry paymentEntry, String receiptNumber) {
        String details = "Paiement ajout\u00e9 pour " + currentRecord.getChildFullName()
                + " - " + CurrencyUtil.formatAmount(paymentEntry.getAmount())
                + " - Re\u00e7u " + receiptNumber;

        activityLogDao.log(getCurrentUserId(), "CREATE_PAYMENT_ENTRY", "payment_entries", paymentEntry.getId(), details);
    }

    private void logReceiptAction(Receipt receipt) {
        String details = "Donn\u00e9es du re\u00e7u cr\u00e9\u00e9es pour " + currentRecord.getChildFullName()
                + " - " + receipt.getReceiptNumber();

        activityLogDao.log(getCurrentUserId(), "CREATE_RECEIPT", "receipts", receipt.getId(), details);
    }
}
