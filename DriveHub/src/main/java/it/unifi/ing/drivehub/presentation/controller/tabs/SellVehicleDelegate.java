package it.unifi.ing.drivehub.presentation.controller.tabs;

import it.unifi.ing.drivehub.presentation.core.UiGateway;
import it.unifi.ing.drivehub.presentation.core.UiModels;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public final class SellVehicleDelegate {
    private final UiGateway gateway;
    private final ComboBox<UiModels.BrandItem> brandComboBox;
    private final ComboBox<UiModels.ModelItem> modelComboBox;
    private final TextField newVinField;
    private final TextField newPlateField;
    private final TextField newKmField;
    private final TextField newVehicleYearField;
    private final TextField newPriceField;
    private final Label valuationQuoteLabel;
    private final Label messageLabel;

    public SellVehicleDelegate(UiGateway gateway,
                               ComboBox<UiModels.BrandItem> brandComboBox,
                               ComboBox<UiModels.ModelItem> modelComboBox,
                               TextField newVinField,
                               TextField newPlateField,
                               TextField newKmField,
                               TextField newVehicleYearField,
                               TextField newPriceField,
                               Label valuationQuoteLabel,
                               Label messageLabel) {
        this.gateway = gateway;
        this.brandComboBox = brandComboBox;
        this.modelComboBox = modelComboBox;
        this.newVinField = newVinField;
        this.newPlateField = newPlateField;
        this.newKmField = newKmField;
        this.newVehicleYearField = newVehicleYearField;
        this.newPriceField = newPriceField;
        this.valuationQuoteLabel = valuationQuoteLabel;
        this.messageLabel = messageLabel;
    }

    public void refreshAsync() {
        if (brandComboBox != null) {
            CompletableFuture.supplyAsync(gateway::allBrands)
                    .thenAccept(brands -> Platform.runLater(() -> brandComboBox.setItems(FXCollections.observableArrayList(brands))));
        }
    }

    public void onBrandSelected() {
        UiModels.BrandItem brand = brandComboBox.getValue();
        if (brand == null) {
            modelComboBox.getItems().clear();
            return;
        }
        CompletableFuture.supplyAsync(() -> gateway.allModels().stream()
                .filter(m -> m.brand().equalsIgnoreCase(brand.name()))
                .toList())
                .thenAccept(models -> Platform.runLater(() -> {
                    modelComboBox.setItems(FXCollections.observableArrayList(models));
                    modelComboBox.getSelectionModel().clearSelection();
                    newPriceField.clear();
                    if (valuationQuoteLabel != null) valuationQuoteLabel.setText("");
                }));
    }

    public void onModelSelected() {
        newPriceField.clear();
        if (valuationQuoteLabel != null) valuationQuoteLabel.setText("");
    }

    public void calculateValuationQuote() {
        UiModels.BrandItem brand = brandComboBox.getValue();
        UiModels.ModelItem model = modelComboBox.getValue();
        if (brand == null || model == null) {
            showError("Seleziona Marchio e Modello dal catalogo prima di calcolare la valutazione.");
            return;
        }

        String vin = newVinField.getText();
        if (vin == null || vin.isBlank()) {
            showError("Inserisci il numero di telaio (VIN).");
            return;
        }
        if (vin.trim().length() > 17) {
            showError("Il numero di telaio (VIN) non può superare 17 caratteri.");
            return;
        }

        String plate = newPlateField.getText() != null ? newPlateField.getText().trim() : "";
        if (plate.isBlank()) plate = null;

        String kmStr = newKmField.getText();
        if (kmStr == null || kmStr.isBlank()) {
            showError("Inserisci i chilometri percorsi.");
            return;
        }
        int km;
        try {
            km = Integer.parseInt(kmStr.trim());
            if (km < 0) {
                showError("I chilometri non possono essere negativi.");
                return;
            }
        } catch (NumberFormatException e) {
            showError("I chilometri devono essere un numero intero valido.");
            return;
        }

        if (plate == null && km != 0) {
            showError("Se il veicolo non ha targa, obbligatoriamente i chilometri devono essere a 0.");
            return;
        }

        String yearStr = newVehicleYearField.getText();
        if (yearStr == null || yearStr.isBlank()) {
            showError("Inserisci l'anno di immatricolazione del veicolo.");
            return;
        }
        int vehicleYear;
        int currentYear = LocalDate.now().getYear();
        try {
            vehicleYear = Integer.parseInt(yearStr.trim());
            if (vehicleYear < 1900 || vehicleYear > currentYear) {
                showError("L'anno del veicolo deve essere compreso tra 1900 e " + currentYear + ".");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Inserisci un anno valido.");
            return;
        }

        final String finalPlate = plate;
        final int finalKm = km;
        final int finalYear = vehicleYear;

        CompletableFuture.supplyAsync(() -> gateway.quoteValuation(brand.name(), model.name(), finalPlate, finalKm, finalYear))
                .thenAccept(estimated -> Platform.runLater(() -> {
                    newPriceField.setText(estimated.toPlainString());
                    if (valuationQuoteLabel != null) {
                        valuationQuoteLabel.setText("Valutazione calcolata dal sistema: " + estimated + " € (-20% usato se targata, -5%/anno, -5%/10.000 km)");
                    }
                    showSuccess("Valutazione automatica completata con successo: " + estimated + " €");
                })).exceptionally(ex -> {
                    Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    return null;
                });
    }

    public void submitVehicleForSale(long clientId, Runnable onSuccess) {
        String priceText = newPriceField.getText();
        if (priceText == null || priceText.isBlank()) {
            showError("Calcola prima la valutazione del veicolo.");
            return;
        }

        BigDecimal price = new BigDecimal(priceText);
        UiModels.BrandItem brand = brandComboBox.getValue();
        UiModels.ModelItem model = modelComboBox.getValue();
        String vin = newVinField.getText().trim();
        String plate = newPlateField.getText() != null ? newPlateField.getText().trim() : null;
        if (plate != null && plate.isBlank()) plate = null;
        int km = Integer.parseInt(newKmField.getText().trim());
        int vehicleYear = Integer.parseInt(newVehicleYearField.getText().trim());

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Confermi l'invio della proposta di vendita alla concessionaria?\n\n"
                        + "Veicolo: " + brand.name() + " " + model.name() + "\n"
                        + "VIN: " + vin + "\n"
                        + "Targa: " + (plate != null ? plate : "Da immatricolare") + "\n"
                        + "Chilometri: " + km + " km\n"
                        + "Anno immatricolazione: " + vehicleYear + "\n"
                        + "Valutazione stimata (accredito previsto se approvato): " + price + " €\n\n"
                        + "Il veicolo sarà registrato con stato 'WAITING' in attesa dell'approvazione di un venditore.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Conferma Proposta di Vendita");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            UiModels.VehicleSaleSubmission sub = new UiModels.VehicleSaleSubmission(
                    brand.name(), model.name(), model.releaseYear(), vin, plate, km, price, vehicleYear
            );
            CompletableFuture.runAsync(() -> gateway.submitVehicleForSale(clientId, sub))
                    .thenRun(() -> Platform.runLater(() -> {
                        showSuccess("Proposta inviata con successo! Il veicolo è in attesa di approvazione ('WAITING').");
                        clearForm();
                        if (onSuccess != null) onSuccess.run();
                    })).exceptionally(ex -> {
                        Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                        return null;
                    });
        }
    }

    private void clearForm() {
        newVinField.clear();
        newPlateField.clear();
        newKmField.clear();
        newVehicleYearField.clear();
        newPriceField.clear();
        if (valuationQuoteLabel != null) valuationQuoteLabel.setText("");
        modelComboBox.getSelectionModel().clearSelection();
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
