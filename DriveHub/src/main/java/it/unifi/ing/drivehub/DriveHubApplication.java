package it.unifi.ing.drivehub;

import it.unifi.ing.drivehub.business.core.PaymentGateway;
import it.unifi.ing.drivehub.business.core.PaymentGatewayResult;
import it.unifi.ing.drivehub.business.security.Pbkdf2PasswordHasher;
import it.unifi.ing.drivehub.business.services.AuthService;
import it.unifi.ing.drivehub.business.services.CatalogService;
import it.unifi.ing.drivehub.business.services.CheckoutService;
import it.unifi.ing.drivehub.business.services.DashboardService;
import it.unifi.ing.drivehub.business.services.InventoryService;
import it.unifi.ing.drivehub.business.services.InventoryActivityFeed;
import it.unifi.ing.drivehub.business.services.PaymentService;
import it.unifi.ing.drivehub.business.services.PricingService;
import it.unifi.ing.drivehub.business.services.PurchaseProposalService;
import it.unifi.ing.drivehub.business.services.RentalService;
import it.unifi.ing.drivehub.business.services.SalesService;
import it.unifi.ing.drivehub.business.services.TestDriveService;
import it.unifi.ing.drivehub.business.strategies.PricingStrategy;
import it.unifi.ing.drivehub.business.strategies.StandardPricingStrategy;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.dao.postgres.DatabaseBootstrap;
import it.unifi.ing.drivehub.dao.postgres.DatabaseConfig;
import it.unifi.ing.drivehub.dao.postgres.DriverManagerDataSource;
import it.unifi.ing.drivehub.dao.postgres.PostgresDaoFactory;
import it.unifi.ing.drivehub.presentation.core.ApplicationContext;
import it.unifi.ing.drivehub.presentation.core.ServiceUiGateway;
import it.unifi.ing.drivehub.presentation.core.UiGateway;
import it.unifi.ing.drivehub.presentation.navigation.NavigationManager;
import it.unifi.ing.drivehub.presentation.navigation.Route;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.sql.DataSource;
import java.util.UUID;

/** JavaFX composition root. No controller knows how services or JDBC are built. */
public final class DriveHubApplication extends Application {
    private ApplicationContext context;

    @Override
    public void start(Stage stage) {
        try {
            context = new ApplicationContext(createGateway());
            NavigationManager.initialize(stage, context);
            NavigationManager.get().show(Route.WELCOME);
        } catch (RuntimeException failure) {
            showStartupFailure(stage, failure);
        }
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }

    private static UiGateway createGateway() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment();
        DataSource dataSource = new DriverManagerDataSource(config);
        new DatabaseBootstrap(dataSource, config.seedDemoData()).initialize();
        DaoFactory daoFactory = new PostgresDaoFactory(dataSource);

        PricingStrategy pricingStrategy = new StandardPricingStrategy();
        String outcome = System.getProperty("drivehub.payment.outcome",
                System.getenv().getOrDefault("DRIVEHUB_PAYMENT_OUTCOME", "approved"));
        if (!outcome.equals("approved") && !outcome.equals("declined")) {
            throw new IllegalArgumentException("drivehub.payment.outcome: usare approved o declined");
        }
        PaymentGateway demoPaymentGateway = (amount, method) -> outcome.equals("declined")
                ? PaymentGatewayResult.rejected("Rifiuto dimostrativo configurato")
                : PaymentGatewayResult.approved("DEMO-" + UUID.randomUUID());
        InventoryService inventory = new InventoryService(daoFactory);
        InventoryActivityFeed feed = new InventoryActivityFeed();
        inventory.addObserver(feed);

        RentalService rentals = new RentalService(daoFactory, pricingStrategy);
        SalesService sales = new SalesService(daoFactory, pricingStrategy);
        PaymentService payments = new PaymentService(daoFactory, demoPaymentGateway);
        CheckoutService checkout = new CheckoutService(daoFactory, rentals, sales, payments);
        return new ServiceUiGateway(
                new AuthService(daoFactory, new Pbkdf2PasswordHasher()),
                new CatalogService(daoFactory),
                rentals,
                new TestDriveService(daoFactory),
                new PurchaseProposalService(daoFactory),
                new PricingService(daoFactory, pricingStrategy),
                inventory,
                new DashboardService(daoFactory, feed), checkout);
    }

    private static void showStartupFailure(Stage stage, RuntimeException failure) {
        Label title = new Label("DriveHub non può collegarsi al database");
        title.getStyleClass().add("startup-error-title");
        Label guidance = new Label("""
                Verifica che PostgreSQL sia avviato e che DRIVEHUB_DB_PASSWORD sia impostata.
                Puoi usare: cp .env.example .env && docker compose up -d

                Dettaglio: %s
                """.formatted("Configurazione o connessione non disponibile"));
        guidance.setWrapText(true);
        VBox content = new VBox(18, title, guidance);
        content.setPadding(new Insets(36));
        Scene scene = new Scene(content, 720, 320);
        var css = DriveHubApplication.class.getResource(
                "presentation/css/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setTitle("DriveHub — configurazione richiesta");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
