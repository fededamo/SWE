package it.unifi.ing.drivehub.presentation.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.math.BigDecimal;

/** Controller kept reusable for a future external payment adapter; the demo is synchronous. */
public final class PaymentDialogController {
    @FXML private Label amountLabel;
    @FXML private Label referenceLabel;
    @FXML private ComboBox<String> methodCombo;
    @FXML private Label messageLabel;
    private boolean confirmed;

    @FXML
    private void initialize() {
        methodCombo.setItems(FXCollections.observableArrayList("Carta demo", "Bonifico demo"));
        methodCombo.getSelectionModel().selectFirst();
    }

    public void configure(BigDecimal amount, String reference) {
        amountLabel.setText("€ " + amount.setScale(2, java.math.RoundingMode.HALF_UP));
        referenceLabel.setText(reference);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String selectedMethod() {
        return methodCombo.getValue();
    }

    @FXML
    private void confirm(javafx.event.ActionEvent event) {
        confirmed = true;
        close(event);
    }

    @FXML
    private void cancel(javafx.event.ActionEvent event) {
        confirmed = false;
        close(event);
    }

    private void close(javafx.event.ActionEvent event) {
        Node source = (Node) event.getSource();
        ((Stage) source.getScene().getWindow()).close();
    }
}
