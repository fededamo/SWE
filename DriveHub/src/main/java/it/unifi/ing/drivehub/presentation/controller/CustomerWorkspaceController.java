package it.unifi.ing.drivehub.presentation.controller;

import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;
import it.unifi.ing.drivehub.presentation.core.UiModels;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public final class CustomerWorkspaceController extends AbstractController {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label userLabel;
    @FXML private TabPane workspaceTabs;
    @FXML private TextField catalogSearchField;
    @FXML private ComboBox<VehiclePurpose> vehicleTypeCombo;
    @FXML private TextField maxPriceField;
    @FXML private TableView<UiModels.VehicleItem> catalogTable;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogPlateColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogModelColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogTypeColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogMileageColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogPriceColumn;
    @FXML private TableColumn<UiModels.VehicleItem, String> catalogStatusColumn;
    @FXML private Label testDriveVehicleLabel;
    @FXML private DatePicker testDriveDatePicker;
    @FXML private TextField testDriveTimeField;
    @FXML private TableView<UiModels.TestDriveItem> testDriveTable;
    @FXML private TableColumn<UiModels.TestDriveItem, String> testDriveCodeColumn;
    @FXML private TableColumn<UiModels.TestDriveItem, String> testDriveVehicleColumn;
    @FXML private TableColumn<UiModels.TestDriveItem, String> testDriveDateColumn;
    @FXML private TableColumn<UiModels.TestDriveItem, String> testDriveStatusColumn;
    @FXML private Label rentalVehicleLabel;
    @FXML private DatePicker rentalStartPicker;
    @FXML private DatePicker rentalEndPicker;
    @FXML private Label rentalQuoteLabel;
    @FXML private Label rentalMessageLabel;
    @FXML private TableView<UiModels.RentalItem> rentalTable;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalCodeColumn;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalBookedVehicleColumn;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalDatesColumn;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalAmountColumn;
    @FXML private TableColumn<UiModels.RentalItem, String> rentalStatusColumn;
    @FXML private TextField salePlateField;
    @FXML private TextField saleBrandField;
    @FXML private TextField saleModelField;
    @FXML private TextField saleYearField;
    @FXML private TextField saleMileageField;
    @FXML private TextField saleRequestedPriceField;
    @FXML private Label saleMessageLabel;
    @FXML private Label globalMessageLabel;

    private UiModels.VehicleItem selectedVehicle;

    @FXML
    private void initialize() {
        vehicleTypeCombo.setItems(FXCollections.observableArrayList(VehiclePurpose.FOR_SALE, VehiclePurpose.RENTAL));
        catalogPlateColumn.setCellValueFactory(v -> property(v.getValue().plate()));
        catalogModelColumn.setCellValueFactory(v -> property(v.getValue().displayName()));
        catalogTypeColumn.setCellValueFactory(v -> property(v.getValue().purpose()));
        catalogMileageColumn.setCellValueFactory(v -> property(v.getValue().mileage()));
        catalogPriceColumn.setCellValueFactory(v -> property(money(v.getValue().price())));
        catalogStatusColumn.setCellValueFactory(v -> property(v.getValue().status()));

        testDriveCodeColumn.setCellValueFactory(v -> property(v.getValue().code()));
        testDriveVehicleColumn.setCellValueFactory(v -> property(v.getValue().vehicle()));
        testDriveDateColumn.setCellValueFactory(v -> property(v.getValue().scheduledAt().format(DATE_TIME)));
        testDriveStatusColumn.setCellValueFactory(v -> property(v.getValue().status()));

        rentalCodeColumn.setCellValueFactory(v -> property(v.getValue().code()));
        rentalBookedVehicleColumn.setCellValueFactory(v -> property(v.getValue().vehicle()));
        rentalDatesColumn.setCellValueFactory(v -> property(v.getValue().startDate() + " → " + v.getValue().endDate()));
        rentalAmountColumn.setCellValueFactory(v -> property(money(v.getValue().amount())));
        rentalStatusColumn.setCellValueFactory(v -> property(v.getValue().status()));
        rentalStartPicker.setValue(LocalDate.now().plusDays(1));
        rentalEndPicker.setValue(LocalDate.now().plusDays(3));
        testDriveDatePicker.setValue(LocalDate.now().plusDays(1));
        testDriveTimeField.setText("10:00");
    }

    @Override
    protected void onContextReady() {
        userLabel.setText(session().displayName() + " · Customer");
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            searchCatalog();
            testDriveTable.setItems(FXCollections.observableArrayList(gateway().customerTestDrives(session().userId())));
            rentalTable.setItems(FXCollections.observableArrayList(gateway().customerRentals(session().userId())));
            globalMessageLabel.setText("");
        } catch (RuntimeException exception) {
            showError(globalMessageLabel, exception);
        }
    }

    @FXML
    private void searchCatalog() {
        try {
            BigDecimal max = maxPriceField.getText().isBlank() ? null : decimal(maxPriceField.getText(), "Prezzo massimo");
            List<UiModels.VehicleItem> result = gateway().searchCatalog(
                    catalogSearchField.getText(), vehicleTypeCombo.getValue(), max);
            catalogTable.setItems(FXCollections.observableArrayList(result));
        } catch (RuntimeException exception) {
            showError(globalMessageLabel, exception);
        }
    }

    @FXML
    private void selectForTestDrive() {
        selectedVehicle = requireSelectedVehicle();
        testDriveVehicleLabel.setText(selectedVehicle.displayName() + " · " + selectedVehicle.plate());
        workspaceTabs.getSelectionModel().select(1);
    }

    @FXML
    private void selectForRental() {
        UiModels.VehicleItem vehicle = requireSelectedVehicle();
        if (vehicle.purpose() != VehiclePurpose.RENTAL) {
            throw new IllegalArgumentException("Seleziona un veicolo destinato al noleggio");
        }
        selectedVehicle = vehicle;
        rentalVehicleLabel.setText(vehicle.displayName() + " · " + vehicle.plate());
        workspaceTabs.getSelectionModel().select(2);
    }

    @FXML
    private void selectForPurchase() {
        try {
            UiModels.VehicleItem vehicle = requireSelectedVehicle();
            if (vehicle.purpose() != VehiclePurpose.FOR_SALE) {
                throw new IllegalArgumentException("Seleziona un veicolo in vendita");
            }
            ChoiceDialog<String> kind = new ChoiceDialog<>("Prenotazione con acconto", "Prenotazione con acconto", "Acquisto completo");
            kind.setTitle("Prenota / acquista");
            kind.setHeaderText(vehicle.displayName() + " — " + money(vehicle.price()));
            Optional<String> chosenKind = kind.showAndWait();
            if (chosenKind.isEmpty()) return;
            boolean fullPurchase = chosenKind.get().startsWith("Acquisto");
            BigDecimal paymentAmount = fullPurchase ? vehicle.price()
                    : vehicle.price().multiply(new BigDecimal("0.10"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            Optional<String> method = askPaymentMethod(paymentAmount,
                    (fullPurchase ? "Acquisto " : "Acconto ") + vehicle.plate());
            if (method.isEmpty()) return;
            gateway().reserveOrPurchase(session().userId(), vehicle.id(), fullPurchase, method.get());
            showSuccess(globalMessageLabel, fullPurchase ? "Acquisto registrato" : "Prenotazione e acconto registrati");
            refresh();
        } catch (RuntimeException exception) {
            showError(globalMessageLabel, exception);
        }
    }

    @FXML
    private void bookTestDrive() {
        try {
            if (selectedVehicle == null) throw new IllegalArgumentException("Seleziona prima un veicolo");
            LocalTime time = LocalTime.parse(testDriveTimeField.getText().trim());
            gateway().bookTestDrive(session().userId(), selectedVehicle.id(), LocalDateTime.of(testDriveDatePicker.getValue(), time));
            showSuccess(globalMessageLabel, "Test drive prenotato");
            refresh();
        } catch (DateTimeParseException exception) {
            showError(globalMessageLabel, new IllegalArgumentException("Ora non valida: usare HH:mm"));
        } catch (RuntimeException exception) {
            showError(globalMessageLabel, exception);
        }
    }

    @FXML
    private void quoteRental() {
        try {
            ensureRentalSelection();
            BigDecimal quote = gateway().quoteRental(selectedVehicle.id(), rentalStartPicker.getValue(), rentalEndPicker.getValue());
            rentalQuoteLabel.setText(money(quote));
        } catch (RuntimeException exception) {
            showError(rentalMessageLabel, exception);
        }
    }

    @FXML
    private void rentAndPay() {
        try {
            ensureRentalSelection();
            BigDecimal amount = gateway().quoteRental(selectedVehicle.id(),
                    rentalStartPicker.getValue(), rentalEndPicker.getValue());
            Optional<String> method = askPaymentMethod(amount, "Noleggio " + selectedVehicle.plate());
            if (method.isEmpty()) return;
            gateway().rentAndPay(session().userId(), selectedVehicle.id(), rentalStartPicker.getValue(), rentalEndPicker.getValue(), method.get());
            showSuccess(rentalMessageLabel, "Pagamento simulato riuscito e noleggio confermato");
            refresh();
            workspaceTabs.getSelectionModel().select(3);
        } catch (RuntimeException exception) {
            showError(rentalMessageLabel, exception);
        }
    }

    @FXML
    private void cancelRental() {
        try {
            UiModels.RentalItem selected = rentalTable.getSelectionModel().getSelectedItem();
            if (selected == null) throw new IllegalArgumentException("Seleziona una prenotazione");
            gateway().cancelRental(session().userId(), session().role(), selected.id());
            showSuccess(globalMessageLabel, "Prenotazione annullata");
            refresh();
        } catch (RuntimeException exception) {
            showError(globalMessageLabel, exception);
        }
    }

    @FXML
    private void submitVehicleSale() {
        try {
            UiModels.VehicleSaleRequest request = new UiModels.VehicleSaleRequest(
                    salePlateField.getText(), saleBrandField.getText(), saleModelField.getText(),
                    Math.toIntExact(wholeNumber(saleYearField.getText(), "Anno")),
                    wholeNumber(saleMileageField.getText(), "Chilometraggio"),
                    decimal(saleRequestedPriceField.getText(), "Valutazione richiesta"));
            gateway().submitVehicleSale(session().userId(), request);
            showSuccess(saleMessageLabel, "Richiesta inviata al reparto vendite");
            clearSaleForm();
        } catch (RuntimeException exception) {
            showError(saleMessageLabel, exception);
        }
    }

    private UiModels.VehicleItem requireSelectedVehicle() {
        UiModels.VehicleItem vehicle = catalogTable.getSelectionModel().getSelectedItem();
        if (vehicle == null) throw new IllegalArgumentException("Seleziona un veicolo dal catalogo");
        return vehicle;
    }

    private void ensureRentalSelection() {
        if (selectedVehicle == null || selectedVehicle.purpose() != VehiclePurpose.RENTAL) {
            throw new IllegalArgumentException("Seleziona prima un veicolo a noleggio");
        }
        if (rentalStartPicker.getValue() == null || rentalEndPicker.getValue() == null) {
            throw new IllegalArgumentException("Indica entrambe le date");
        }
    }

    private Optional<String> askPaymentMethod(BigDecimal amount, String reference) {
        try {
            FXMLLoader loader = new FXMLLoader(CustomerWorkspaceController.class.getResource(
                    "../view/PaymentDialog.fxml"));
            Parent root = loader.load();
            PaymentDialogController controller = loader.getController();
            controller.configure(amount, reference);

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(catalogTable.getScene().getWindow());
            dialog.setTitle("Pagamento dimostrativo");
            dialog.setResizable(false);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
            return controller.isConfirmed()
                    ? Optional.ofNullable(controller.selectedMethod())
                    : Optional.empty();
        } catch (IOException exception) {
            throw new IllegalStateException("Impossibile aprire il pagamento", exception);
        }
    }

    private void clearSaleForm() {
        salePlateField.clear(); saleBrandField.clear(); saleModelField.clear(); saleYearField.clear();
        saleMileageField.clear(); saleRequestedPriceField.clear();
    }

    private static SimpleStringProperty property(Object value) {
        return new SimpleStringProperty(value == null ? "—" : String.valueOf(value));
    }

    private static String money(BigDecimal value) {
        return value == null ? "—" : "€ " + value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
