package it.unifi.ing.drivehub.presentation.controller.tabs;

import it.unifi.ing.drivehub.domain.vehicles.VehicleState;
import it.unifi.ing.drivehub.presentation.core.UiGateway;
import it.unifi.ing.drivehub.presentation.core.UiModels;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class CatalogTabDelegate {
    private final UiGateway gateway;
    private final TextField searchField;
    private final ComboBox<VehicleState> stateCombo;
    private final TableView<UiModels.VehicleItem> catalogTable;
    private final Label messageLabel;
    private final Consumer<Integer> tabSwitcher;

    private UiModels.VehicleItem selectedVehicle;

    public CatalogTabDelegate(UiGateway gateway,
                              TextField searchField,
                              ComboBox<VehicleState> stateCombo,
                              TableView<UiModels.VehicleItem> catalogTable,
                              TableColumn<UiModels.VehicleItem, String> plateCol,
                              TableColumn<UiModels.VehicleItem, String> vinCol,
                              TableColumn<UiModels.VehicleItem, String> modelCol,
                              TableColumn<UiModels.VehicleItem, String> yearCol,
                              TableColumn<UiModels.VehicleItem, String> mileageCol,
                              TableColumn<UiModels.VehicleItem, String> priceCol,
                              TableColumn<UiModels.VehicleItem, String> stateCol,
                              Label messageLabel,
                              Consumer<Integer> tabSwitcher,
                              Consumer<UiModels.VehicleItem> onVehicleSelected) {
        this.gateway = gateway;
        this.searchField = searchField;
        this.stateCombo = stateCombo;
        this.catalogTable = catalogTable;
        this.messageLabel = messageLabel;
        this.tabSwitcher = tabSwitcher;

        stateCombo.setItems(FXCollections.observableArrayList(VehicleState.SALE, VehicleState.RENT));
        stateCombo.setValue(VehicleState.SALE);

        plateCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().plate() != null ? c.getValue().plate() : "Da immatricolare"));
        vinCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vin()));
        modelCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().displayName()));
        yearCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().year())));
        mileageCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().kilometers() + " km"));
        priceCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().price() + " €"));
        stateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().state().name()));

        catalogTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            this.selectedVehicle = newV;
            if (onVehicleSelected != null) {
                onVehicleSelected.accept(newV);
            }
        });
    }

    public void searchCatalogAsync() {
        String query = searchField.getText();
        VehicleState state = stateCombo.getValue();
        CompletableFuture.supplyAsync(() -> gateway.searchCatalog(query, state))
                .thenAccept(items -> Platform.runLater(() -> catalogTable.setItems(FXCollections.observableArrayList(items))))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError(ex.getMessage()));
                    return null;
                });
    }

    public void selectForPurchase(long clientId, Runnable onPurchaseSuccess) {
        if (selectedVehicle == null) {
            showError("Seleziona un veicolo dal catalogo per procedere con l'acquisto.");
            return;
        }
        if (selectedVehicle.state() != VehicleState.SALE) {
            showError("Solo i veicoli con stato 'SALE' possono essere acquistati.");
            return;
        }

        BigDecimal deposit = selectedVehicle.price().multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Confermi l'avvio della procedura di acquisto per " + selectedVehicle.displayName() + "?\n\n"
                        + "Prezzo totale del veicolo: " + selectedVehicle.price() + " €\n"
                        + "Acconto richiesto (20%): " + deposit + " €\n\n"
                        + "L'importo di " + deposit + " € verrà scalato dal tuo conto corrente bancario e il veicolo verrà riservato (stato 'BLOCKED') in attesa dell'autorizzazione di un venditore.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Conferma Acquisto e Versamento Acconto (20%)");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            CompletableFuture.runAsync(() -> gateway.requestPurchase(clientId, selectedVehicle.id(), deposit))
                    .thenRun(() -> Platform.runLater(() -> {
                        showSuccess("Acconto di " + deposit + " € versato con successo! Veicolo riservato (BLOCKED) e richiesta registrata.");
                        if (onPurchaseSuccess != null) onPurchaseSuccess.run();
                        if (tabSwitcher != null) tabSwitcher.accept(1);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                        return null;
                    });
        }
    }

    public UiModels.VehicleItem getSelectedVehicle() {
        return selectedVehicle;
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
