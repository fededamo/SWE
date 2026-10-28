package it.unifi.ing.drivehub.presentation.controller;

import it.unifi.ing.drivehub.domain.vehicles.VehicleState;
import it.unifi.ing.drivehub.presentation.controller.tabs.*;
import it.unifi.ing.drivehub.presentation.core.UiModels;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class CustomerWorkspaceController extends AbstractController {

    @FXML private Label userLabel;
    @FXML private TabPane workspaceTabs;

    // Catalogo
    @FXML private TextField catalogSearchField;
    @FXML private ComboBox<VehicleState> vehicleStateCombo;
    @FXML private TableView<UiModels.VehicleItem> catalogTable;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogPlateColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogVinColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogModelColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogYearColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogMileageColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogPriceColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogStateColumn;

    // Richieste e Acquisti
    @FXML private TableView<UiModels.RequestItem> requestsTable;
    @FXML private TableColumn<UiModels.RequestItem, String> requestVehicleColumn;
    @FXML private TableColumn<UiModels.RequestItem, String> requestTypeColumn;
    @FXML private TableColumn<UiModels.RequestItem, String> requestDepositColumn;
    @FXML private TableColumn<UiModels.RequestItem, String> requestExpDateColumn;

    @FXML private TableView<UiModels.SaleItem> salesTable;
    @FXML private TableColumn<UiModels.SaleItem, String> saleVehicleColumn;
    @FXML private TableColumn<UiModels.SaleItem, String> saleAmountColumn;
    @FXML private TableColumn<UiModels.SaleItem, String> saleDateColumn;
    @FXML private TableColumn<UiModels.SaleItem, String> saleStatusColumn;

    // Noleggi
    @FXML private Label rentalSelectedVehicleLabel;
    @FXML private DatePicker rentalStartDatePicker;
    @FXML private DatePicker rentalEndDatePicker;
    @FXML private Label rentalQuoteLabel;
    @FXML private TableView<UiModels.RentItem> rentalsTable;
    @FXML private TableColumn<UiModels.RentItem, String> rentalVehicleColumn;
    @FXML private TableColumn<UiModels.RentItem, String> rentalPeriodColumn;
    @FXML private TableColumn<UiModels.RentItem, String> rentalAmountColumn;

    // Test Drive
    @FXML private Label testDriveSelectedVehicleLabel;
    @FXML private DatePicker testDriveDatePicker;
    @FXML private ComboBox<String> testDriveHourCombo;
    @FXML private TableView<UiModels.TestDriveItem> testDrivesTable;
    @FXML private TableColumn<UiModels.TestDriveItem, String> testDriveVehicleColumn;
    @FXML private TableColumn<UiModels.TestDriveItem, String> testDriveDateColumn;
    @FXML private TableColumn<UiModels.TestDriveItem, String> testDriveTimeColumn;

    // Vendita veicolo da cliente
    @FXML private ComboBox<UiModels.BrandItem> brandComboBox;
    @FXML private ComboBox<UiModels.ModelItem> modelComboBox;
    @FXML private TextField newVinField;
    @FXML private TextField newPlateField;
    @FXML private TextField newKmField;
    @FXML private TextField newVehicleYearField;
    @FXML private TextField newPriceField;
    @FXML private Label valuationQuoteLabel;

    // Conto Bancario
    @FXML private Label ibanLabel;
    @FXML private Label balanceLabel;
    @FXML private TextField depositAmountField;

    // Profilo e Account
    @FXML private PasswordField emailCurrentPasswordField;
    @FXML private TextField newEmailField;
    @FXML private PasswordField pwdCurrentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField deleteAccountPasswordField;

    @FXML private Label globalMessageLabel;

    private CatalogTabDelegate catalogDelegate;
    private PurchasesTabDelegate purchasesDelegate;
    private RentalsAndTestDrivesDelegate rentalsDelegate;
    private SellVehicleDelegate sellVehicleDelegate;
    private AccountTabDelegate accountDelegate;

    @FXML
    private void initialize() {
        // I delegati verranno completamente istanziati in onContextReady quando il gateway è disponibile
    }

    @Override
    protected void onContextReady() {
        if (!context.session().isLoggedIn()) {
            return;
        }

        userLabel.setText(session().displayName() + " (" + session().email() + ")");

        rentalsDelegate = new RentalsAndTestDrivesDelegate(gateway(),
                rentalSelectedVehicleLabel, rentalStartDatePicker, rentalEndDatePicker, rentalQuoteLabel, rentalsTable,
                rentalVehicleColumn, rentalPeriodColumn, rentalAmountColumn,
                testDriveSelectedVehicleLabel, testDriveDatePicker, testDriveHourCombo, testDrivesTable,
                testDriveVehicleColumn, testDriveDateColumn, testDriveTimeColumn,
                globalMessageLabel);

        catalogDelegate = new CatalogTabDelegate(gateway(),
                catalogSearchField, vehicleStateCombo, catalogTable,
                catalogPlateColumn, catalogVinColumn, catalogModelColumn, catalogYearColumn,
                catalogMileageColumn, catalogPriceColumn, catalogStateColumn,
                globalMessageLabel,
                index -> workspaceTabs.getSelectionModel().select(index),
                vehicle -> rentalsDelegate.onVehicleSelected(vehicle));

        purchasesDelegate = new PurchasesTabDelegate(gateway(),
                requestsTable, requestVehicleColumn, requestTypeColumn, requestDepositColumn, requestExpDateColumn,
                salesTable, saleVehicleColumn, saleAmountColumn, saleDateColumn, saleStatusColumn,
                globalMessageLabel);

        sellVehicleDelegate = new SellVehicleDelegate(gateway(),
                brandComboBox, modelComboBox, newVinField, newPlateField, newKmField,
                newVehicleYearField, newPriceField, valuationQuoteLabel, globalMessageLabel);

        accountDelegate = new AccountTabDelegate(gateway(),
                ibanLabel, balanceLabel, depositAmountField,
                newEmailField, emailCurrentPasswordField, pwdCurrentPasswordField,
                newPasswordField, deleteAccountPasswordField, globalMessageLabel);

        refresh();
    }

    @FXML
    public void refresh() {
        clearMessages();
        if (session() == null) return;
        long clientId = session().userId();
        if (catalogDelegate != null) catalogDelegate.searchCatalogAsync();
        if (purchasesDelegate != null) purchasesDelegate.refreshAsync(clientId);
        if (rentalsDelegate != null) rentalsDelegate.refreshAsync(clientId);
        if (sellVehicleDelegate != null) sellVehicleDelegate.refreshAsync();
        if (accountDelegate != null) accountDelegate.refreshAsync(clientId);
    }

    @FXML
    public void searchCatalog() {
        clearMessages();
        if (catalogDelegate != null) catalogDelegate.searchCatalogAsync();
    }

    @FXML
    public void selectForPurchase() {
        clearMessages();
        if (catalogDelegate != null) {
            catalogDelegate.selectForPurchase(session().userId(), this::refresh);
        }
    }

    @FXML
    public void selectForRental() {
        workspaceTabs.getSelectionModel().select(2);
    }

    @FXML
    public void selectForTestDrive() {
        workspaceTabs.getSelectionModel().select(2);
    }

    @FXML
    public void payFullBalance() {
        clearMessages();
        if (purchasesDelegate != null) {
            purchasesDelegate.payFullBalance(session().userId(), this::refresh);
        }
    }

    @FXML
    public void cancelRequestWithPenalty() {
        clearMessages();
        if (purchasesDelegate != null) {
            purchasesDelegate.cancelRequestWithPenalty(session().userId(), this::refresh);
        }
    }

    @FXML
    public void updateRentalQuotePreview() {
        if (rentalsDelegate != null) rentalsDelegate.updateRentalQuotePreview();
    }

    @FXML
    public void confirmRental() {
        clearMessages();
        if (rentalsDelegate != null) {
            rentalsDelegate.confirmRental(session().userId(), this::refresh);
        }
    }

    @FXML
    public void onTestDriveDateSelected() {
        if (rentalsDelegate != null) rentalsDelegate.onTestDriveDateSelected();
    }

    @FXML
    public void confirmTestDrive() {
        clearMessages();
        if (rentalsDelegate != null) {
            rentalsDelegate.confirmTestDrive(session().userId(), this::refresh);
        }
    }

    @FXML
    public void onBrandSelected() {
        clearMessages();
        if (sellVehicleDelegate != null) sellVehicleDelegate.onBrandSelected();
    }

    @FXML
    public void onModelSelected() {
        if (sellVehicleDelegate != null) sellVehicleDelegate.onModelSelected();
    }

    @FXML
    public void calculateValuationQuote() {
        clearMessages();
        if (sellVehicleDelegate != null) sellVehicleDelegate.calculateValuationQuote();
    }

    @FXML
    public void submitVehicleForSale() {
        clearMessages();
        if (sellVehicleDelegate != null) {
            sellVehicleDelegate.submitVehicleForSale(session().userId(), () -> {
                refresh();
                workspaceTabs.getSelectionModel().select(1);
            });
        }
    }

    @FXML
    public void depositFunds() {
        clearMessages();
        if (accountDelegate != null) {
            accountDelegate.depositFunds(session().userId(), this::refresh);
        }
    }

    @FXML
    public void updateEmail() {
        clearMessages();
        if (accountDelegate != null) {
            accountDelegate.updateEmail(session().userId(), newEmail -> {
                userLabel.setText(session().displayName() + " (" + newEmail + ")");
            });
        }
    }

    @FXML
    public void updatePassword() {
        clearMessages();
        if (accountDelegate != null) {
            accountDelegate.updatePassword(session().userId());
        }
    }

    @FXML
    public void deleteAccount() {
        clearMessages();
        if (accountDelegate != null) {
            accountDelegate.deleteAccount(session().userId(), this::logout);
        }
    }

    private void clearMessages() {
        if (globalMessageLabel != null) {
            globalMessageLabel.setText("");
            globalMessageLabel.getStyleClass().removeAll("danger-text", "success-text");
        }
    }
}
