package it.unifi.ing.drivehub.presentation;

import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;
import it.unifi.ing.drivehub.presentation.core.*;
import it.unifi.ing.drivehub.presentation.navigation.NavigationManager;
import it.unifi.ing.drivehub.presentation.navigation.Route;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Real JavaFX controls with a controlled application port; not a database end-to-end test. */
@Tag("gui")
class JavaFxSmokeTest {
    private static final String VIEWS = "/it/unifi/ing/drivehub/presentation/view/";
    private static final UiModels.VehicleItem RENTAL = new UiModels.VehicleItem(11, "UI111AA", "Veicolo noleggio demo",
            VehiclePurpose.RENTAL, 5000, new BigDecimal("50.00"), VehicleStatus.AVAILABLE, null);
    private static final UiModels.VehicleItem SALE = new UiModels.VehicleItem(12, "UI222AA", "Veicolo vendita demo",
            VehiclePurpose.FOR_SALE, 8000, new BigDecimal("20000.00"), VehicleStatus.AVAILABLE, null);

    @BeforeAll
    static void startToolkit() throws Exception {
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.complete(null); });
        ready.get(15, TimeUnit.SECONDS);
    }

    @AfterEach
    void closeWindows() throws Exception {
        onFx(() -> List.copyOf(Window.getWindows()).forEach(Window::hide));
    }

    // No @AfterAll call to Platform.exit(): after the last window is hidden the
    // native application thread may already have stopped. Calling any lifecycle
    // API then waits forever in PlatformImpl.waitForStart. Surefire terminates
    // this dedicated GUI-test fork once the test class returns.

    @Test
    @DisplayName("FT-AUTH-03/05, FT-NAV-01: routing reale dei tre ruoli, guard e logout")
    void allRoutesLoadAndLogoutRemovesSession() throws Exception {
        onFx(() -> {
            Stub stub = new Stub();
            ApplicationContext context = new ApplicationContext(stub.gateway());
            Stage stage = new Stage();
            NavigationManager.initialize(stage, context);
            NavigationManager navigation = NavigationManager.get();
            for (Route route : List.of(Route.WELCOME, Route.LOGIN, Route.REGISTER)) navigation.show(route);
            screenshot(stage, "registration.png");
            assertThrows(IllegalStateException.class, () -> navigation.show(Route.CUSTOMER));
            for (Role role : Role.values()) {
                context.session().open(session(role));
                navigation.showHomeForCurrentRole();
                assertTrue(stage.getTitle().contains(role.name()));
                Route forbidden = role == Role.CUSTOMER ? Route.MANAGER : Route.CUSTOMER;
                assertThrows(SecurityException.class, () -> navigation.show(forbidden));
                screenshot(stage, role.name().toLowerCase() + "-workspace.png");
                navigation.logout();
                assertTrue(context.session().current().isEmpty());
                assertTrue(stage.getTitle().contains("WELCOME"));
                assertThrows(IllegalStateException.class, () -> navigation.show(Route.CUSTOMER));
            }
        });
    }

    @Test
    @DisplayName("FT-CAT-02, FT-TD-02: stato vuoto e selezioni/date non valide restano correggibili")
    void emptyCatalogAndInvalidSelectionShowMessages() throws Exception {
        onFx(() -> {
            Stub stub = new Stub();
            stub.catalog = List.of();
            View view = customer(stub);
            assertEquals("Nessun veicolo corrisponde ai filtri", ((Label) view.table("catalogTable").getPlaceholder()).getText());
            button(view.root, "Noleggia").fire();
            assertTrue(view.label("globalMessageLabel").getText().contains("Seleziona"));
            button(view.root, "Prenota test drive").fire();
            assertTrue(view.label("globalMessageLabel").getText().contains("Seleziona"));
            assertEquals(0, stub.rentalCalls);
        });
    }

    @Test
    @DisplayName("FT-RENT-04: annullamento e riattivazione durante dialog non scrivono")
    void cancelledRentalAndReentrantClickNeverSubmit() throws Exception {
        onFx(() -> {
            Stub stub = new Stub();
            View view = customer(stub);
            selectRental(view);
            AtomicReference<Throwable> error = new AtomicReference<>();
            dialogAction(error, dialog -> {
                assertEquals(0, stub.rentalCalls);
                button(view.root, "Conferma e paga").fire();
                assertEquals(2, Window.getWindows().stream().filter(Window::isShowing).count());
                button(dialog.getScene().getRoot(), "Annulla").fire();
            });
            button(view.root, "Conferma e paga").fire();
            rethrow(error);
            assertEquals(0, stub.rentalCalls);
            assertTrue(view.label("rentalMessageLabel").getText().contains("annullato"));
        });
    }

    @Test
    @DisplayName("FT-RENT-01, FT-PAY-01: conferma dialog invia una sola richiesta con importo approvato")
    void confirmedRentalSubmitsOnceAndPreservesSuccess() throws Exception {
        onFx(() -> {
            Stub stub = new Stub();
            View view = customer(stub);
            selectRental(view);
            confirmNextPayment();
            button(view.root, "Conferma e paga").fire();
            assertEquals(1, stub.rentalCalls);
            assertEquals(new BigDecimal("100.00"), stub.approvedAmount);
            assertTrue(view.label("globalMessageLabel").getText().contains("attende la presa in carico"));
            button(view.root, "Conferma e paga").fire();
            assertEquals(1, stub.rentalCalls, "Completed selection must not submit twice");
        });
    }

    @Test
    @DisplayName("FT-RENT-03, FT-PAY-02: rifiuto business visibile senza messaggio di successo")
    void rentalRejectionRemainsVisible() throws Exception {
        onFx(() -> {
            Stub stub = new Stub();
            stub.reject = true;
            View view = customer(stub);
            selectRental(view);
            confirmNextPayment();
            button(view.root, "Conferma e paga").fire();
            assertEquals(1, stub.rentalCalls);
            assertTrue(view.label("rentalMessageLabel").getText().contains("rifiutato"));
            assertTrue(view.label("rentalMessageLabel").getStyleClass().contains("danger-text"));
        });
    }

    @Test
    @DisplayName("FT-SALE-01: acconto quotato dal servizio e annullamento prima di qualsiasi mutazione")
    void saleDialogShowsAuthoritativeQuoteAndCanCancel() throws Exception {
        onFx(() -> {
            Stub stub = new Stub();
            View view = customer(stub);
            view.table("catalogTable").getSelectionModel().select(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            dialogAction(error, dialog -> {
                ((Button) ((DialogPane) dialog.getScene().getRoot()).lookupButton(ButtonType.OK)).fire();
                dialogAction(error, payment -> {
                    assertEquals("€ 1900.00", ((Label) payment.getScene().lookup("#amountLabel")).getText());
                    assertEquals(0, stub.saleCalls);
                    screenshot(payment, "payment-dialog.png");
                    button(payment.getScene().getRoot(), "Annulla").fire();
                });
            });
            button(view.root, "Prenota / acquista").fire();
            rethrow(error);
            assertEquals(0, stub.saleCalls);
            assertTrue(view.label("globalMessageLabel").getText().contains("annullato"));
        });
    }

    @Test
    @DisplayName("FT-SALE-01/03: vendita confermata e rifiutata attraversano PaymentDialog")
    void saleSuccessAndRejectionUseRealDialog() throws Exception {
        onFx(() -> {
            for (boolean reject : List.of(false, true)) {
                Stub stub = new Stub(); stub.reject = reject;
                View view = customer(stub);
                view.table("catalogTable").getSelectionModel().select(1);
                AtomicReference<Throwable> error = new AtomicReference<>();
                dialogAction(error, kind -> {
                    ((Button) ((DialogPane) kind.getScene().getRoot()).lookupButton(ButtonType.OK)).fire();
                    confirmNextPayment();
                });
                button(view.root, "Prenota / acquista").fire();
                rethrow(error);
                assertEquals(1, stub.saleCalls);
                assertEquals(new BigDecimal("1900.00"), stub.approvedAmount);
                assertTrue(view.label("globalMessageLabel").getText().contains(reject ? "rifiutato" : "registrati"));
                view.stage.close();
            }
        });
    }

    @Test @Tag("postgres")
    @DisplayName("IT-UI-PG-01: composition root, login dei tre ruoli e checkout JavaFX su PostgreSQL")
    void realApplicationPersistsConfirmedRental() throws Exception {
        try (var database = new it.unifi.ing.drivehub.dao.postgres.PostgresTestDatabase()) {
            new it.unifi.ing.drivehub.dao.postgres.DatabaseBootstrap(database.dataSource(), true).initialize();
            var factory = new it.unifi.ing.drivehub.dao.postgres.PostgresDaoFactory(database.dataSource());
            var auth = new it.unifi.ing.drivehub.business.services.AuthService(factory,
                    new it.unifi.ing.drivehub.business.security.Pbkdf2PasswordHasher());
            for (Role role : Role.values()) {
                auth.register(new it.unifi.ing.drivehub.business.services.RegistrationRequest(
                        String.format("E2EUSER%09d", role.ordinal()), "Demo", role.name(),
                        role.name().toLowerCase() + "@example.invalid", "+39000000000", role, null), "demo-test-123".toCharArray());
            }
            java.util.Map<String, String> settings = java.util.Map.of(
                    "drivehub.db.url", database.jdbcUrl(), "drivehub.db.user", System.getenv("DRIVEHUB_TEST_DB_USER"),
                    "drivehub.db.password", System.getenv("DRIVEHUB_TEST_DB_PASSWORD"), "drivehub.db.seed-demo", "true",
                    "drivehub.payment.outcome", "approved");
            java.util.Map<String, String> original = new java.util.HashMap<>();
            settings.forEach((key, value) -> { original.put(key, System.getProperty(key)); System.setProperty(key, value); });
            try {
                onFx(() -> {
                    var app = new it.unifi.ing.drivehub.DriveHubApplication(); Stage stage = new Stage(); app.start(stage);
                    assertTrue(stage.getTitle().contains("WELCOME"));
                    for (Role role : Role.values()) {
                        NavigationManager.get().show(Route.LOGIN);
                        Parent root = stage.getScene().getRoot();
                        ((TextField) root.lookup("#emailField")).setText(role.name().toLowerCase() + "@example.invalid");
                        ((PasswordField) root.lookup("#passwordField")).setText("demo-test-123"); button(root, "ACCEDI").fire();
                        assertTrue(stage.getTitle().contains(role.name()), "Role login must reach its workspace");
                        if (role == Role.CUSTOMER) {
                            Parent workspace = stage.getScene().getRoot(); workspace.applyCss(); workspace.layout();
                            @SuppressWarnings("unchecked") TableView<UiModels.VehicleItem> table = (TableView<UiModels.VehicleItem>) workspace.lookup("#catalogTable");
                            assertEquals(2, table.getItems().size());
                            table.getSelectionModel().select(table.getItems().stream().filter(v -> v.purpose() == VehiclePurpose.RENTAL).findFirst().orElseThrow());
                            screenshot(stage, "postgres-customer-catalog.png");
                            button(workspace, "Noleggia").fire(); confirmNextPayment(); button(workspace, "Conferma e paga").fire();
                            assertTrue(((Label) workspace.lookup("#globalMessageLabel")).getText().contains("attende la presa in carico"));
                            screenshot(stage, "postgres-rental-confirmed.png");
                        } else screenshot(stage, "postgres-" + role.name().toLowerCase() + "-workspace.png");
                        button(stage.getScene().getRoot(), "Logout").fire();
                        assertTrue(stage.getTitle().contains("WELCOME"));
                    }
                    app.stop(); stage.close();
                });
                try (var unit = factory.begin()) {
                    assertEquals(1, unit.rentals().findAll().size());
                    var payment = unit.payments().findAll().getFirst();
                    assertEquals(it.unifi.ing.drivehub.domain.sales.PaymentStatus.COMPLETED, payment.status());
                    assertEquals(unit.rentals().findAll().getFirst().totalPrice(), payment.amount());
                }
            } finally {
                original.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
            }
        }
    }

    private static void selectRental(View view) {
        view.table("catalogTable").getSelectionModel().select(0);
        button(view.root, "Noleggia").fire();
    }

    private static View customer(Stub stub) throws Exception {
        FXMLLoader loader = new FXMLLoader(JavaFxSmokeTest.class.getResource(VIEWS + "CustomerWorkspace.fxml"));
        Parent root = loader.load();
        ApplicationContext context = new ApplicationContext(stub.gateway());
        context.session().open(session(Role.CUSTOMER));
        ((ContextAware) loader.getController()).setApplicationContext(context);
        Stage stage = new Stage(); stage.setScene(new Scene(root)); stage.show();
        return new View(loader, root, stage);
    }

    private static UiModels.Session session(Role role) {
        return new UiModels.Session(1, "Utente dimostrativo", "ui@drivehub.test", role);
    }

    private static void confirmNextPayment() {
        Platform.runLater(() -> button(lastDialog().getScene().getRoot(), "Conferma pagamento").fire());
    }

    private static Stage lastDialog() {
        return (Stage) Window.getWindows().stream().filter(Window::isShowing).reduce((a, b) -> b).orElseThrow();
    }

    private static void dialogAction(AtomicReference<Throwable> error, FxConsumer action) {
        Platform.runLater(() -> {
            Stage dialog = lastDialog();
            try { action.accept(dialog); }
            catch (Throwable failure) { error.set(failure); dialog.close(); }
        });
    }

    private static void rethrow(AtomicReference<Throwable> error) {
        if (error.get() != null) throw new AssertionError("Dialog interaction failed", error.get());
    }

    private static Button button(Node root, String text) {
        for (Node node : descendants(root)) {
            if (node instanceof Button button && text.equals(button.getText())) return button;
        }
        throw new AssertionError("Button not found: " + text);
    }

    private static List<Node> descendants(Node root) {
        List<Node> nodes = new ArrayList<>(); nodes.add(root);
        if (root instanceof Parent parent) parent.getChildrenUnmodifiable().forEach(child -> nodes.addAll(descendants(child)));
        if (root instanceof TabPane tabs) tabs.getTabs().forEach(tab -> { if (tab.getContent() != null) nodes.addAll(descendants(tab.getContent())); });
        return nodes;
    }

    private static void screenshot(Stage stage, String name) throws Exception {
        String directory = System.getProperty("drivehub.ui.screenshots");
        if (directory == null) return;
        stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
        WritableImage snapshot = stage.getScene().snapshot(null);
        BufferedImage image = new BufferedImage((int) snapshot.getWidth(), (int) snapshot.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
        Files.createDirectories(Path.of(directory));
        ImageIO.write(image, "png", Path.of(directory, name).toFile());
    }

    private static void onFx(FxRunnable action) throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> { try { action.run(); result.complete(null); } catch (Throwable error) { result.completeExceptionally(error); } });
        result.get(25, TimeUnit.SECONDS);
    }

    @FunctionalInterface interface FxRunnable { void run() throws Exception; }
    @FunctionalInterface interface FxConsumer { void accept(Stage stage) throws Exception; }
    record View(FXMLLoader loader, Parent root, Stage stage) {
        Label label(String name) { return (Label) loader.getNamespace().get(name); }
        @SuppressWarnings("unchecked") TableView<UiModels.VehicleItem> table(String name) { return (TableView<UiModels.VehicleItem>) loader.getNamespace().get(name); }
    }

    private static class Stub {
        List<UiModels.VehicleItem> catalog = List.of(RENTAL, SALE);
        int rentalCalls; int saleCalls; boolean reject; BigDecimal approvedAmount;
        UiGateway gateway() {
            return (UiGateway) Proxy.newProxyInstance(UiGateway.class.getClassLoader(), new Class<?>[]{UiGateway.class}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "searchCatalog", "inventory" -> catalog;
                    case "quoteRental" -> new BigDecimal("100.00");
                    case "quotePurchase" -> new BigDecimal("1900.00");
                    case "dashboard" -> new UiModels.Dashboard(2, 0, BigDecimal.ZERO, 0);
                    case "rentAndPay" -> { rentalCalls++; approvedAmount = (BigDecimal) args[5]; if (reject) throw new ConflictException("Pagamento rifiutato: simulazione controllata"); yield null; }
                    case "reserveOrPurchase" -> { saleCalls++; approvedAmount = (BigDecimal) args[4]; if (reject) throw new ConflictException("Pagamento rifiutato: simulazione controllata"); yield null; }
                    default -> method.getReturnType() == List.class ? List.of() : null;
                };
            });
        }
    }
}
