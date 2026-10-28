package it.unifi.ing.drivehub.presentation.controller.tabs;

import it.unifi.ing.drivehub.presentation.core.UiGateway;
import it.unifi.ing.drivehub.presentation.core.UiModels;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public final class PurchasesTabDelegate {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UiGateway gateway;
    private final TableView<UiModels.RequestItem> requestsTable;
    private final TableView<UiModels.SaleItem> salesTable;
    private final Label messageLabel;

    public PurchasesTabDelegate(UiGateway gateway,
                                TableView<UiModels.RequestItem> requestsTable,
                                TableColumn<UiModels.RequestItem, String> reqVehCol,
                                TableColumn<UiModels.RequestItem, String> reqTypeCol,
                                TableColumn<UiModels.RequestItem, String> reqDepCol,
                                TableColumn<UiModels.RequestItem, String> reqExpCol,
                                TableView<UiModels.SaleItem> salesTable,
                                TableColumn<UiModels.SaleItem, String> saleVehCol,
                                TableColumn<UiModels.SaleItem, String> saleAmountCol,
                                TableColumn<UiModels.SaleItem, String> saleDateCol,
                                TableColumn<UiModels.SaleItem, String> saleStatusCol,
                                Label messageLabel) {
        this.gateway = gateway;
        this.requestsTable = requestsTable;
        this.salesTable = salesTable;
        this.messageLabel = messageLabel;

        reqVehCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vehicleDesc()));
        reqTypeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isPurchase() ? "Acquisto (Acconto 20%)" : "Proposta vendita auto"));
        reqDepCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().deposit() + " €"));
        reqExpCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().expDate() != null ? c.getValue().expDate().format(DATE_FMT) : "In attesa sblocco"));

        saleVehCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vehicleDesc()));
        saleAmountCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().amount() + " €"));
        saleDateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().date().format(DATE_FMT)));
        saleStatusCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isDefaultFailure() ? "Mancato saldo (Penale 20% trattenuta)"
                        : (c.getValue().isAcquisition() ? "Vendita a concessionaria" : "Acquisto Concluso")));
    }

    public void refreshAsync(long clientId) {
        CompletableFuture.supplyAsync(() -> gateway.clientRequests(clientId))
                .thenAccept(requests -> Platform.runLater(() -> requestsTable.setItems(FXCollections.observableArrayList(requests))));

        CompletableFuture.supplyAsync(() -> gateway.clientPurchases(clientId))
                .thenAccept(sales -> Platform.runLater(() -> salesTable.setItems(FXCollections.observableArrayList(sales))));
    }

    public void payFullBalance(long clientId, Runnable onSuccess) {
        UiModels.RequestItem req = requestsTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showError("Seleziona una richiesta dalla tabella.");
            return;
        }
        if (!req.isPurchase()) {
            showError("Questa richiesta è una proposta di vendita da te effettuata, non un acquisto.");
            return;
        }
        if (req.expDate() == null) {
            showError("La richiesta non è ancora stata autorizzata da un venditore. Attendi che venga autorizzata.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Sei sicuro di voler saldare l'importo rimanente per " + req.vehicleDesc() + "?\nIl saldo verrà scalato dal tuo conto corrente bancario.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Conferma Saldo Acquisto");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            long salesmanId = req.salesmanId() != null ? req.salesmanId() : 1L;
            CompletableFuture.runAsync(() -> gateway.completePurchase(clientId, req.id(), salesmanId))
                    .thenRun(() -> Platform.runLater(() -> {
                        showSuccess("Acquisto completato con successo! Il veicolo è tuo.");
                        if (onSuccess != null) onSuccess.run();
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                        return null;
                    });
        }
    }

    public void cancelRequestWithPenalty(long clientId, Runnable onSuccess) {
        UiModels.RequestItem req = requestsTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showError("Seleziona una richiesta dalla tabella.");
            return;
        }
        if (!req.isPurchase()) {
            showError("Solo le richieste di acquisto possono essere annullate qui.");
            return;
        }

        BigDecimal deposit = req.deposit();
        BigDecimal penalty = deposit.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal refund = deposit.subtract(penalty);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Attenzione: annullando la richiesta d'acquisto verrà trattenuto il 20% della caparra ("
                        + penalty + " €) come penale per la concessionaria,\ne ti verrà rimborsato il restante 80% ("
                        + refund + " €) sul tuo conto corrente.\n\nVuoi procedere?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Conferma Annullamento con Penale");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            CompletableFuture.runAsync(() -> gateway.cancelPurchase(clientId, req.id()))
                    .thenRun(() -> Platform.runLater(() -> {
                        showSuccess("Richiesta annullata: 20% trattenuto e " + refund + " € riaccreditati sul conto.");
                        if (onSuccess != null) onSuccess.run();
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                        return null;
                    });
        }
    }

    private void showError(String msg) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle("-fx-text-fill: #e53e3e;");
        }
    }

    private void showSuccess(String msg) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle("-fx-text-fill: #38a169;");
        }
    }
}
