package it.unifi.ing.drivehub.presentation.controller.tabs;

import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.presentation.core.UiGateway;
import javafx.application.Platform;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class AccountTabDelegate {
    private final UiGateway gateway;
    private final Label ibanLabel;
    private final Label balanceLabel;
    private final TextField depositAmountField;

    private final TextField newEmailField;
    private final PasswordField emailCurrentPasswordField;
    private final PasswordField pwdCurrentPasswordField;
    private final PasswordField newPasswordField;
    private final PasswordField deleteAccountPasswordField;
    private final Label messageLabel;

    public AccountTabDelegate(UiGateway gateway,
                              Label ibanLabel,
                              Label balanceLabel,
                              TextField depositAmountField,
                              TextField newEmailField,
                              PasswordField emailCurrentPasswordField,
                              PasswordField pwdCurrentPasswordField,
                              PasswordField newPasswordField,
                              PasswordField deleteAccountPasswordField,
                              Label messageLabel) {
        this.gateway = gateway;
        this.ibanLabel = ibanLabel;
        this.balanceLabel = balanceLabel;
        this.depositAmountField = depositAmountField;
        this.newEmailField = newEmailField;
        this.emailCurrentPasswordField = emailCurrentPasswordField;
        this.pwdCurrentPasswordField = pwdCurrentPasswordField;
        this.newPasswordField = newPasswordField;
        this.deleteAccountPasswordField = deleteAccountPasswordField;
        this.messageLabel = messageLabel;
    }

    public void refreshAsync(long clientId) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return gateway.getBankAccount(clientId);
            } catch (Exception ex) {
                return null;
            }
        }).thenAccept(acc -> Platform.runLater(() -> {
            if (acc != null) {
                ibanLabel.setText(acc.iban());
                balanceLabel.setText(acc.balance().toPlainString() + " €");
            } else {
                ibanLabel.setText("Non disponibile");
                balanceLabel.setText("0.00 €");
            }
        }));
    }

    public void depositFunds(long clientId, Runnable onSuccess) {
        String amountText = depositAmountField.getText();
        if (amountText == null || amountText.isBlank()) {
            showError("Inserisci l'importo da versare.");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("L'importo del versamento deve essere positivo.");
                return;
            }
        } catch (NumberFormatException ex) {
            showError("Inserisci un importo numerico valido.");
            return;
        }

        CompletableFuture.runAsync(() -> gateway.depositFunds(clientId, amount))
                .thenRun(() -> Platform.runLater(() -> {
                    showSuccess("Versamento di " + amount + " € completato con successo!");
                    depositAmountField.clear();
                    if (onSuccess != null) onSuccess.run();
                })).exceptionally(ex -> {
                    Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    return null;
                });
    }

    public void updateEmail(long clientId, Consumer<String> onEmailUpdated) {
        String currentPwd = emailCurrentPasswordField.getText();
        String newEmail = newEmailField.getText();
        if (currentPwd == null || currentPwd.isBlank() || newEmail == null || newEmail.isBlank()) {
            showError("Inserisci la password attuale e il nuovo indirizzo email.");
            return;
        }

        CompletableFuture.runAsync(() -> gateway.updateEmail(clientId, Role.CLIENT, currentPwd.toCharArray(), newEmail))
                .thenRun(() -> Platform.runLater(() -> {
                    showSuccess("Email aggiornata con successo in: " + newEmail);
                    emailCurrentPasswordField.clear();
                    newEmailField.clear();
                    if (onEmailUpdated != null) onEmailUpdated.accept(newEmail);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    return null;
                });
    }

    public void updatePassword(long clientId) {
        String currentPwd = pwdCurrentPasswordField.getText();
        String newPwd = newPasswordField.getText();
        if (currentPwd == null || currentPwd.isBlank() || newPwd == null || newPwd.isBlank()) {
            showError("Inserisci la password attuale e la nuova password.");
            return;
        }

        CompletableFuture.runAsync(() -> gateway.updatePassword(clientId, Role.CLIENT, currentPwd.toCharArray(), newPwd.toCharArray()))
                .thenRun(() -> Platform.runLater(() -> {
                    showSuccess("Password aggiornata con successo!");
                    pwdCurrentPasswordField.clear();
                    newPasswordField.clear();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    return null;
                });
    }

    public void deleteAccount(long clientId, Runnable onLogout) {
        String currentPwd = deleteAccountPasswordField.getText();
        if (currentPwd == null || currentPwd.isBlank()) {
            showError("Inserisci la tua password attuale per confermare l'eliminazione dell'account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Sei sicuro di voler eliminare definitivamente il tuo account?\nL'operazione è consentita solo se non hai storico di operazioni (Test Drive, Noleggi o Acquisti).",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Elimina Account");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            CompletableFuture.runAsync(() -> gateway.deleteAccount(clientId, Role.CLIENT, currentPwd.toCharArray()))
                    .thenRun(() -> Platform.runLater(() -> {
                        Alert info = new Alert(Alert.AlertType.INFORMATION, "Account eliminato con successo.");
                        info.showAndWait();
                        if (onLogout != null) onLogout.run();
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
