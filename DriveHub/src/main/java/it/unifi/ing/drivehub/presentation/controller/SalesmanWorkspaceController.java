package it.unifi.ing.drivehub.presentation.controller;

import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;
import it.unifi.ing.drivehub.presentation.core.UiModels;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;

public final class SalesmanWorkspaceController extends AbstractController {
    @FXML private Label userLabel;
    @FXML private Label messageLabel;
    @FXML private TableView<UiModels.TestDriveItem> testDriveTable;
    @FXML private TableColumn<UiModels.TestDriveItem, String> tdCodeColumn;
    @FXML private TableColumn<UiModels.TestDriveItem, String> tdCustomerColumn;
    @FXML private TableColumn<UiModels.TestDriveItem, String> tdVehicleColumn;
    @FXML private TableColumn<UiModels.TestDriveItem, String> tdDateColumn;
    @FXML private TableColumn<UiModels.TestDriveItem, String> tdStatusColumn;
    @FXML private TableView<UiModels.RentalItem> rentalTable;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalCodeColumn;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalCustomerColumn;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalVehicleColumn;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalPeriodColumn;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalStatusColumn;
    @FXML private TableView<UiModels.ProposalItem> proposalTable;
    @FXML private TableColumn<UiModels.ProposalItem, String> proposalCodeColumn;
    @FXML private TableColumn<UiModels.ProposalItem, String> proposalCustomerColumn;
    @FXML private TableColumn<UiModels.ProposalItem, String> proposalVehicleColumn;
    @FXML private TableColumn<UiModels.ProposalItem, String> proposalAskedColumn;
    @FXML private TableColumn<UiModels.ProposalItem, String> proposalStatusColumn;
    @FXML private TextField offerAmountField;
    @FXML private TableView<UiModels.VehicleItem> inventoryTable;
    @FXML private TableColumn<UiModels.VehicleItem, String> inventoryPlateColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> inventoryModelColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> inventoryTypeColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> inventoryStatusColumn;
    @FXML private ComboBox<VehicleStatus> inventoryStatusCombo;

    @FXML
    private void initialize() {
        testDriveTable.setPlaceholder(new Label("Nessun elemento disponibile"));
        rentalTable.setPlaceholder(new Label("Nessun elemento disponibile"));
        proposalTable.setPlaceholder(new Label("Nessun elemento disponibile"));
        inventoryTable.setPlaceholder(new Label("Nessun elemento disponibile"));

        tdCodeColumn.setCellValueFactory(v -> property(v.getValue().code()));
        tdCustomerColumn.setCellValueFactory(v -> property(v.getValue().customer()));
        tdVehicleColumn.setCellValueFactory(v -> property(v.getValue().vehicle()));
        tdDateColumn.setCellValueFactory(v -> property(v.getValue().scheduledAt()));
        tdStatusColumn.setCellValueFactory(v -> property(v.getValue().status()));

        rentalCodeColumn.setCellValueFactory(v -> property(v.getValue().code()));
        rentalCustomerColumn.setCellValueFactory(v -> property(v.getValue().customer()));
        rentalVehicleColumn.setCellValueFactory(v -> property(v.getValue().vehicle()));
        rentalPeriodColumn.setCellValueFactory(v -> property(v.getValue().startDate() + " → " + v.getValue().endDate()));
        rentalStatusColumn.setCellValueFactory(v -> property(v.getValue().status()));

        proposalCodeColumn.setCellValueFactory(v -> property(v.getValue().code()));
        proposalCustomerColumn.setCellValueFactory(v -> property(v.getValue().customer()));
        proposalVehicleColumn.setCellValueFactory(v -> property(v.getValue().vehicle()));
        proposalAskedColumn.setCellValueFactory(v -> property(money(v.getValue().requestedAmount())));
        proposalStatusColumn.setCellValueFactory(v -> property(v.getValue().status()));

        inventoryPlateColumn.setCellValueFactory(v -> property(v.getValue().plate()));
        inventoryModelColumn.setCellValueFactory(v -> property(v.getValue().displayName()));
        inventoryTypeColumn.setCellValueFactory(v -> property(v.getValue().purpose()));
        inventoryStatusColumn.setCellValueFactory(v -> property(v.getValue().status()));
        inventoryStatusCombo.setItems(FXCollections.observableArrayList(
                VehicleStatus.MAINTENANCE, VehicleStatus.AVAILABLE));
    }

    @Override
    protected void onContextReady() {
        userLabel.setText(session().displayName() + " · Salesman");
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            testDriveTable.setItems(FXCollections.observableArrayList(
                    gateway().manageableTestDrives(session().userId())));
            rentalTable.setItems(FXCollections.observableArrayList(gateway().manageableRentals(session().userId())));
            proposalTable.setItems(FXCollections.observableArrayList(gateway().proposalsForSalesman(session().userId())));
            inventoryTable.setItems(FXCollections.observableArrayList(gateway().inventory(session().userId())));
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    @FXML
    private void confirmTestDrive() {
        actOnTestDrive("confirm");
    }

    @FXML
    private void startTestDrive() {
        actOnTestDrive("start");
    }

    @FXML
    private void completeTestDrive() {
        actOnTestDrive("complete");
    }

    @FXML
    private void cancelTestDrive() {
        actOnTestDrive("cancel");
    }

    private void actOnTestDrive(String action) {
        try {
            UiModels.TestDriveItem item = require(testDriveTable, "Seleziona un test drive");
            switch (action) {
                case "confirm" -> gateway().confirmTestDrive(session().userId(), item.id());
                case "start" -> gateway().startTestDrive(session().userId(), item.id());
                case "complete" -> gateway().completeTestDrive(session().userId(), item.id());
                case "cancel" -> gateway().cancelTestDrive(session().userId(), session().role(), item.id());
                default -> throw new IllegalStateException("Azione sconosciuta");
            }
            showSuccess(messageLabel, "Test drive aggiornato");
            refresh();
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    @FXML
    private void claimRental() {
        actOnRental("claim");
    }

    @FXML
    private void completeRental() {
        actOnRental("complete");
    }

    @FXML
    private void confirmRental() {
        actOnRental("confirm");
    }

    @FXML
    private void startRental() {
        actOnRental("start");
    }

    @FXML
    private void cancelRental() {
        actOnRental("cancel");
    }

    private void actOnRental(String action) {
        try {
            UiModels.RentalItem item = require(rentalTable, "Seleziona un noleggio");
            switch (action) {
                case "claim" -> gateway().claimRental(session().userId(), item.id());
                case "confirm" -> gateway().confirmRental(session().userId(), item.id());
                case "start" -> gateway().startRental(session().userId(), item.id());
                case "complete" -> gateway().completeRental(session().userId(), item.id());
                case "cancel" -> gateway().cancelRental(session().userId(), session().role(), item.id());
                default -> throw new IllegalStateException("Azione sconosciuta");
            }
            showSuccess(messageLabel, "Noleggio aggiornato");
            refresh();
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    @FXML
    private void submitProposal() {
        try {
            UiModels.ProposalItem item = require(proposalTable, "Seleziona una richiesta cliente");
            BigDecimal amount = decimal(offerAmountField.getText(), "Importo offerto");
            gateway().submitPurchaseProposal(session().userId(), item.id(), amount);
            offerAmountField.clear();
            showSuccess(messageLabel, "Proposta inviata al manager");
            refresh();
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    @FXML
    private void updateVehicleStatus() {
        try {
            UiModels.VehicleItem item = require(inventoryTable, "Seleziona un veicolo");
            VehicleStatus status = inventoryStatusCombo.getValue();
            if (status == null) throw new IllegalArgumentException("Seleziona il nuovo stato");
            gateway().updateVehicleStatus(session().userId(), item.id(), status);
            showSuccess(messageLabel, "Stato inventario aggiornato");
            refresh();
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    private static <T> T require(TableView<T> table, String message) {
        T item = table.getSelectionModel().getSelectedItem();
        if (item == null) throw new IllegalArgumentException(message);
        return item;
    }

    private static SimpleStringProperty property(Object value) {
        return new SimpleStringProperty(value == null ? "—" : String.valueOf(value));
    }

    private static String money(BigDecimal value) {
        return value == null ? "—" : "€ " + value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
