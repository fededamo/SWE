package it.unifi.ing.drivehub.presentation.controller.tabs;

import it.unifi.ing.drivehub.domain.vehicles.VehicleState;
import it.unifi.ing.drivehub.presentation.core.UiGateway;
import it.unifi.ing.drivehub.presentation.core.UiModels;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class RentalsAndTestDrivesDelegate {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final UiGateway gateway;
    private final Label rentalVehicleLabel;
    private final DatePicker rentalStartPicker;
    private final DatePicker rentalEndPicker;
    private final Label rentalQuoteLabel;
    private final TableView<UiModels.RentItem> rentalsTable;

    private final Label testDriveVehicleLabel;
    private final DatePicker testDriveDatePicker;
    private final ComboBox<String> testDriveHourCombo;
    private final TableView<UiModels.TestDriveItem> testDrivesTable;
    private final Label messageLabel;

    private UiModels.VehicleItem selectedVehicle;

    public RentalsAndTestDrivesDelegate(UiGateway gateway,
                                       Label rentalVehicleLabel,
                                       DatePicker rentalStartPicker,
                                       DatePicker rentalEndPicker,
                                       Label rentalQuoteLabel,
                                       TableView<UiModels.RentItem> rentalsTable,
                                       TableColumn<UiModels.RentItem, String> rentVehCol,
                                       TableColumn<UiModels.RentItem, String> rentPeriodCol,
                                       TableColumn<UiModels.RentItem, String> rentAmountCol,
                                       Label testDriveVehicleLabel,
                                       DatePicker testDriveDatePicker,
                                       ComboBox<String> testDriveHourCombo,
                                       TableView<UiModels.TestDriveItem> testDrivesTable,
                                       TableColumn<UiModels.TestDriveItem, String> tdVehCol,
                                       TableColumn<UiModels.TestDriveItem, String> tdDateCol,
                                       TableColumn<UiModels.TestDriveItem, String> tdTimeCol,
                                       Label messageLabel) {
        this.gateway = gateway;
        this.rentalVehicleLabel = rentalVehicleLabel;
        this.rentalStartPicker = rentalStartPicker;
        this.rentalEndPicker = rentalEndPicker;
        this.rentalQuoteLabel = rentalQuoteLabel;
        this.rentalsTable = rentalsTable;
        this.testDriveVehicleLabel = testDriveVehicleLabel;
        this.testDriveDatePicker = testDriveDatePicker;
        this.testDriveHourCombo = testDriveHourCombo;
        this.testDrivesTable = testDrivesTable;
        this.messageLabel = messageLabel;

        rentVehCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vehicleDesc()));
        rentPeriodCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().startDate().format(DATE_FMT) + " - " + c.getValue().endDate().format(DATE_FMT)));
        rentAmountCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().amount() + " €"));

        tdVehCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vehicleDesc()));
        tdDateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().date().format(DATE_FMT)));
        tdTimeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().start().format(TIME_FMT)));

        testDriveHourCombo.setItems(FXCollections.observableArrayList(
                "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00"
        ));
        testDriveHourCombo.setValue("10:00");
    }

    public void onVehicleSelected(UiModels.VehicleItem v) {
        this.selectedVehicle = v;
        if (v != null) {
            rentalVehicleLabel.setText("Veicolo selezionato: " + v.displayName() + " (" + v.price() + " €)");
            testDriveVehicleLabel.setText("Veicolo selezionato: " + v.displayName());
            updateRentalQuotePreview();
            updateRentalCalendarRestrictions();
            updateTestDriveCalendarRestrictions();
        }
    }

    public void refreshAsync(long clientId) {
        CompletableFuture.supplyAsync(() -> gateway.clientRentals(clientId))
                .thenAccept(rents -> Platform.runLater(() -> rentalsTable.setItems(FXCollections.observableArrayList(rents))));

        CompletableFuture.supplyAsync(() -> gateway.clientTestDrives(clientId))
                .thenAccept(tds -> Platform.runLater(() -> testDrivesTable.setItems(FXCollections.observableArrayList(tds))));
    }

    public void updateRentalQuotePreview() {
        if (selectedVehicle == null || selectedVehicle.state() != VehicleState.RENT) {
            if (rentalQuoteLabel != null) rentalQuoteLabel.setText("Seleziona un veicolo RENT");
            return;
        }
        LocalDate start = rentalStartPicker.getValue();
        LocalDate end = rentalEndPicker.getValue();
        if (start != null && end != null && !end.isBefore(start)) {
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            CompletableFuture.supplyAsync(() -> {
                try {
                    return gateway.quoteRentalPrice(selectedVehicle.id(), days);
                } catch (Exception ex) {
                    return null;
                }
            }).thenAccept(quote -> Platform.runLater(() -> {
                if (quote != null && rentalQuoteLabel != null) {
                    rentalQuoteLabel.setText("Preventivo totale per " + days + " gg: " + quote + " € (Sconto 5% ogni 5 gg applicato)");
                    rentalQuoteLabel.setStyle("-fx-text-fill: #3182ce;");
                }
            }));
        }
    }

    public void confirmRental(long clientId, Runnable onSuccess) {
        if (selectedVehicle == null || selectedVehicle.state() != VehicleState.RENT) {
            showError("Seleziona dal catalogo un veicolo con modalità 'RENT'.");
            return;
        }
        LocalDate start = rentalStartPicker.getValue();
        LocalDate end = rentalEndPicker.getValue();
        if (start == null || end == null) {
            showError("Seleziona sia la data di inizio che di fine noleggio.");
            return;
        }
        if (end.isBefore(start)) {
            showError("La data di fine non può precedere la data di inizio.");
            return;
        }

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        CompletableFuture.supplyAsync(() -> gateway.quoteRentalPrice(selectedVehicle.id(), days))
                .thenAccept(quote -> Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Confermi la prenotazione del noleggio per " + selectedVehicle.displayName() + "?\n\n"
                                    + "Periodo: dal " + start.format(DATE_FMT) + " al " + end.format(DATE_FMT) + " (" + days + " giorni)\n"
                                    + "Costo totale noleggio: " + quote + " €\n\n"
                                    + "L'importo di " + quote + " € verrà addebitato immediatamente sul tuo conto corrente.",
                            ButtonType.YES, ButtonType.NO);
                    alert.setTitle("Conferma Prenotazione Noleggio");
                    if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                        CompletableFuture.runAsync(() -> gateway.bookRental(clientId, selectedVehicle.id(), start, end))
                                .thenRun(() -> Platform.runLater(() -> {
                                    showSuccess("Noleggio confermato per " + days + " giorni! Importo di " + quote + " € addebitato sul tuo conto corrente.");
                                    if (onSuccess != null) onSuccess.run();
                                }))
                                .exceptionally(ex -> {
                                    Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                                    return null;
                                });
                    }
                })).exceptionally(ex -> {
                    Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    return null;
                });
    }

    public void onTestDriveDateSelected() {
        if (selectedVehicle == null) {
            testDriveHourCombo.getItems().clear();
            return;
        }
        LocalDate date = testDriveDatePicker.getValue();
        if (date == null || date.isBefore(LocalDate.now())) {
            testDriveHourCombo.getItems().clear();
            return;
        }

        long vid = selectedVehicle.id();
        CompletableFuture.runAsync(() -> {
            boolean isRented = gateway.allRentals().stream()
                    .anyMatch(r -> r.vehicleId() == vid && !date.isBefore(r.startDate()) && !date.isAfter(r.endDate()));
            if (isRented) {
                Platform.runLater(() -> {
                    testDriveHourCombo.getItems().clear();
                    showError("Il veicolo è noleggiato per il giorno selezionato: nessuna fascia oraria disponibile.");
                });
                return;
            }

            Set<Integer> bookedHours = gateway.allTestDrives().stream()
                    .filter(td -> td.vehicleId() == vid && td.date().equals(date))
                    .map(td -> td.start().getHour())
                    .collect(Collectors.toSet());

            List<String> available = new ArrayList<>();
            LocalTime now = LocalTime.now();
            boolean isToday = date.equals(LocalDate.now());

            for (int h = 9; h <= 16; h++) {
                if (bookedHours.contains(h)) continue;
                if (isToday && h <= now.getHour()) continue;
                available.add(String.format("%02d:00", h));
            }

            Platform.runLater(() -> {
                testDriveHourCombo.setItems(FXCollections.observableArrayList(available));
                if (!available.isEmpty()) {
                    testDriveHourCombo.setValue(available.get(0));
                } else {
                    testDriveHourCombo.setValue(null);
                    showError("Tutti gli orari per la data selezionata sono già occupati.");
                }
            });
        });
    }

    public void confirmTestDrive(long clientId, Runnable onSuccess) {
        if (selectedVehicle == null) {
            showError("Seleziona un veicolo dal catalogo per il test drive.");
            return;
        }
        LocalDate date = testDriveDatePicker.getValue();
        String hourStr = testDriveHourCombo.getValue();
        if (date == null || hourStr == null || hourStr.isBlank()) {
            showError("Seleziona una data valida e una fascia oraria disponibile per il test drive.");
            return;
        }
        try {
            LocalTime time = LocalTime.parse(hourStr, TIME_FMT);
            CompletableFuture.runAsync(() -> gateway.bookTestDrive(clientId, selectedVehicle.id(), date, time))
                    .thenRun(() -> Platform.runLater(() -> {
                        showSuccess("Test drive prenotato con successo per il " + date.format(DATE_FMT) + " alle ore " + time + " (durata 1 ora).");
                        if (onSuccess != null) onSuccess.run();
                        updateTestDriveCalendarRestrictions();
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                        return null;
                    });
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    public void updateRentalCalendarRestrictions() {
        if (selectedVehicle == null || selectedVehicle.state() != VehicleState.RENT) {
            rentalStartPicker.setDayCellFactory(null);
            rentalEndPicker.setDayCellFactory(null);
            return;
        }
        long vid = selectedVehicle.id();
        CompletableFuture.supplyAsync(() -> gateway.allRentals().stream()
                .filter(r -> r.vehicleId() == vid)
                .toList())
                .thenAccept(rents -> Platform.runLater(() -> {
                    Callback<DatePicker, DateCell> dayCellFactory = picker -> new DateCell() {
                        @Override
                        public void updateItem(LocalDate item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) return;
                            if (item.isBefore(LocalDate.now())) {
                                setDisable(true);
                                return;
                            }
                            for (UiModels.RentItem r : rents) {
                                if (!item.isBefore(r.startDate()) && !item.isAfter(r.endDate())) {
                                    setDisable(true);
                                    setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
                                    setTooltip(new Tooltip("Veicolo già noleggiato per questo giorno"));
                                    return;
                                }
                            }
                        }
                    };
                    rentalStartPicker.setDayCellFactory(dayCellFactory);
                    rentalEndPicker.setDayCellFactory(dayCellFactory);
                }));
    }

    public void updateTestDriveCalendarRestrictions() {
        if (selectedVehicle == null) {
            testDriveDatePicker.setDayCellFactory(null);
            return;
        }
        long vid = selectedVehicle.id();
        CompletableFuture.supplyAsync(() -> gateway.allRentals().stream()
                .filter(r -> r.vehicleId() == vid)
                .toList())
                .thenAccept(rentals -> Platform.runLater(() -> {
                    Callback<DatePicker, DateCell> dayCellFactory = picker -> new DateCell() {
                        @Override
                        public void updateItem(LocalDate item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) return;
                            if (item.isBefore(LocalDate.now())) {
                                setDisable(true);
                                return;
                            }
                            for (UiModels.RentItem r : rentals) {
                                if (!item.isBefore(r.startDate()) && !item.isAfter(r.endDate())) {
                                    setDisable(true);
                                    setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
                                    setTooltip(new Tooltip("Veicolo a noleggio: nessuna fascia oraria disponibile"));
                                    return;
                                }
                            }
                        }
                    };
                    testDriveDatePicker.setDayCellFactory(dayCellFactory);
                    onTestDriveDateSelected();
                }));
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
