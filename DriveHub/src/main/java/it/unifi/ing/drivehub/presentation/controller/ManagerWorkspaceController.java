package it.unifi.ing.drivehub.presentation.controller;

import it.unifi.ing.drivehub.presentation.core.UiModels;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;

public final class ManagerWorkspaceController extends AbstractController {
    @FXML private Label userLabel;
    @FXML private Label messageLabel;
    @FXML private Label totalVehiclesLabel;
    @FXML private Label activeRentalsLabel;
    @FXML private Label revenueLabel;
    @FXML private Label pendingProposalsLabel;
    @FXML private TableView<UiModels.ActivityItem> activityTable;
    @FXML private TableColumn<UiModels.ActivityItem, String> activityTypeColumn;
    @FXML private TableColumn<UiModels.ActivityItem, String> activityDescriptionColumn;
    @FXML private TableColumn<UiModels.ActivityItem, String> activityAmountColumn;
    @FXML private TableColumn<UiModels.ActivityItem, String> activityDateColumn;
    @FXML private TextField orderBrandField;
    @FXML private TextField orderModelField;
    @FXML private TextField orderYearField;
    @FXML private TextField orderQuantityField;
    @FXML private TextField orderUnitCostField;
    @FXML private TableView<UiModels.StockOrderItem> stockOrderTable;
    @FXML private TableColumn<UiModels.StockOrderItem, String> orderCodeColumn;
    @FXML private TableColumn<UiModels.StockOrderItem, String> orderModelColumn;
    @FXML private TableColumn<UiModels.StockOrderItem, String> orderQuantityColumn;
    @FXML private TableColumn<UiModels.StockOrderItem, String> orderCostColumn;
    @FXML private TableColumn<UiModels.StockOrderItem, String> orderStatusColumn;
    @FXML private TableView<UiModels.VehicleItem> discountVehicleTable;
    @FXML private TableColumn<UiModels.VehicleItem, String> discountPlateColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> discountModelColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> discountPriceColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> discountValueColumn;
    @FXML private TextField newPriceField;
    @FXML private TextField discountPercentField;
    @FXML private TableView<UiModels.ProposalItem> approvalTable;
    @FXML private TableColumn<UiModels.ProposalItem, String> approvalCodeColumn;
    @FXML private TableColumn<UiModels.ProposalItem, String> approvalSalesmanColumn;
    @FXML private TableColumn<UiModels.ProposalItem, String> approvalCustomerColumn;
    @FXML private TableColumn<UiModels.ProposalItem, String> approvalVehicleColumn;
    @FXML private TableColumn<UiModels.ProposalItem, String> approvalAmountColumn;

    @FXML
    private void initialize() {
        activityTypeColumn.setCellValueFactory(v -> property(v.getValue().type()));
        activityDescriptionColumn.setCellValueFactory(v -> property(v.getValue().description()));
        activityAmountColumn.setCellValueFactory(v -> property(money(v.getValue().amount())));
        activityDateColumn.setCellValueFactory(v -> property(v.getValue().occurredAt()));

        orderCodeColumn.setCellValueFactory(v -> property(v.getValue().code()));
        orderModelColumn.setCellValueFactory(v -> property(v.getValue().model()));
        orderQuantityColumn.setCellValueFactory(v -> property(v.getValue().quantity()));
        orderCostColumn.setCellValueFactory(v -> property(money(v.getValue().unitCost())));
        orderStatusColumn.setCellValueFactory(v -> property(v.getValue().status()));

        discountPlateColumn.setCellValueFactory(v -> property(v.getValue().plate()));
        discountModelColumn.setCellValueFactory(v -> property(v.getValue().displayName()));
        discountPriceColumn.setCellValueFactory(v -> property(money(v.getValue().price())));
        discountValueColumn.setCellValueFactory(v -> property(v.getValue().discountPercentage() == null
                ? "—" : v.getValue().discountPercentage() + "%"));

        approvalCodeColumn.setCellValueFactory(v -> property(v.getValue().code()));
        approvalSalesmanColumn.setCellValueFactory(v -> property(v.getValue().salesman()));
        approvalCustomerColumn.setCellValueFactory(v -> property(v.getValue().customer()));
        approvalVehicleColumn.setCellValueFactory(v -> property(v.getValue().vehicle()));
        approvalAmountColumn.setCellValueFactory(v -> property(money(v.getValue().offeredAmount())));
    }

    @Override
    protected void onContextReady() {
        userLabel.setText(session().displayName() + " · Manager");
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            UiModels.Dashboard dashboard = gateway().dashboard(session().userId());
            totalVehiclesLabel.setText(String.valueOf(dashboard.totalVehicles()));
            activeRentalsLabel.setText(String.valueOf(dashboard.activeRentals()));
            revenueLabel.setText(money(dashboard.recordedRevenue()));
            pendingProposalsLabel.setText(String.valueOf(dashboard.pendingProposals()));
            activityTable.setItems(FXCollections.observableArrayList(
                    gateway().recentActivity(session().userId())));
            stockOrderTable.setItems(FXCollections.observableArrayList(gateway().stockOrders()));
            discountVehicleTable.setItems(FXCollections.observableArrayList(gateway().inventory()));
            approvalTable.setItems(FXCollections.observableArrayList(gateway().proposalsAwaitingManager()));
            messageLabel.setText("");
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    @FXML
    private void createStockOrder() {
        try {
            UiModels.StockOrderRequest request = new UiModels.StockOrderRequest(
                    orderBrandField.getText(), orderModelField.getText(),
                    Math.toIntExact(wholeNumber(orderYearField.getText(), "Anno")),
                    Math.toIntExact(wholeNumber(orderQuantityField.getText(), "Quantità")),
                    decimal(orderUnitCostField.getText(), "Costo unitario"));
            gateway().createStockOrder(session().userId(), request);
            showSuccess(messageLabel, "Ordine registrato");
            clearOrderForm();
            refresh();
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    @FXML
    private void updatePrice() {
        try {
            UiModels.VehicleItem item = requireVehicle();
            gateway().updateSalePrice(session().userId(), item.id(), decimal(newPriceField.getText(), "Prezzo"));
            showSuccess(messageLabel, "Prezzo aggiornato");
            refresh();
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    @FXML
    private void applyDiscount() {
        try {
            UiModels.VehicleItem item = requireVehicle();
            gateway().applyDiscount(session().userId(), item.id(), decimal(discountPercentField.getText(), "Sconto"));
            showSuccess(messageLabel, "Sconto applicato");
            refresh();
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    @FXML
    private void removeDiscount() {
        try {
            UiModels.VehicleItem item = requireVehicle();
            gateway().removeDiscount(session().userId(), item.id());
            showSuccess(messageLabel, "Sconto rimosso");
            refresh();
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    @FXML
    private void approveProposal() {
        decide(true);
    }

    @FXML
    private void rejectProposal() {
        decide(false);
    }

    private void decide(boolean approve) {
        try {
            UiModels.ProposalItem item = approvalTable.getSelectionModel().getSelectedItem();
            if (item == null) throw new IllegalArgumentException("Seleziona una proposta");
            gateway().decidePurchaseProposal(session().userId(), item.id(), approve);
            showSuccess(messageLabel, approve ? "Proposta approvata" : "Proposta rifiutata");
            refresh();
        } catch (RuntimeException exception) {
            showError(messageLabel, exception);
        }
    }

    private UiModels.VehicleItem requireVehicle() {
        UiModels.VehicleItem item = discountVehicleTable.getSelectionModel().getSelectedItem();
        if (item == null) throw new IllegalArgumentException("Seleziona un veicolo");
        return item;
    }

    private void clearOrderForm() {
        orderBrandField.clear(); orderModelField.clear(); orderYearField.clear();
        orderQuantityField.clear(); orderUnitCostField.clear();
    }

    private static SimpleStringProperty property(Object value) {
        return new SimpleStringProperty(value == null ? "—" : String.valueOf(value));
    }

    private static String money(BigDecimal value) {
        return value == null ? "—" : "€ " + value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
