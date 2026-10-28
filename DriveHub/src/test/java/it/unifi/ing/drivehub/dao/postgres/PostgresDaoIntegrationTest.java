package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.users.BankAccount;
import it.unifi.ing.drivehub.business.banking.IbanGenerator;
import it.unifi.ing.drivehub.domain.rentals.Rent;
import it.unifi.ing.drivehub.domain.rentals.TestDrive;
import it.unifi.ing.drivehub.domain.sales.Sale;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class PostgresDaoIntegrationTest extends PostgresDaoIntegrationSupport {

    private DaoFactory daoFactory;

    @BeforeEach
    void createFactory() {
        daoFactory = new PostgresDaoFactory(dataSource);
    }

    @Test
    void persistsAndRehydratesUsersCatalogAndBankAccounts() {
        User client;
        Model model;
        Vehicle saleVehicle;
        BankAccount account;

        try (var work = daoFactory.begin()) {
            client = work.users().save(new it.unifi.ing.drivehub.domain.users.Client(null, "Mario", "Rossi", "mario@example.com", "hash"));
            work.users().save(new it.unifi.ing.drivehub.domain.users.Salesman(null, "Luigi", "Verdi", "luigi@example.com", "hash"));
            work.users().save(new it.unifi.ing.drivehub.domain.users.Manager(null, "Luca", "Bianchi", "luca@example.com", "hash"));

            account = work.bankAccounts().save(new BankAccount(IbanGenerator.generate(), client.requireId(), new BigDecimal("1000.00")));

            work.brands().save(new Brand("Alfa Romeo"));
            model = work.models().save(new Model(null, "Alfa Romeo", "Giulia", new BigDecimal("40000.00"), 2023));
            saleVehicle = work.vehicles().save(new Vehicle(null, model.requireId(), "VIN100", "AA100AA",
                    10000, new BigDecimal("35000.00"), 2023, VehicleState.SALE));

            work.discounts().saveVehicleDiscount(new VehicleDiscount(saleVehicle.requireId(), 15));
            work.commit();
        }

        try (var work = daoFactory.begin()) {
            // Verifica utenti via user view
            User loadedClient = work.users().findByEmail("mario@example.com").orElseThrow();
            assertEquals(client.id(), loadedClient.id());
            assertEquals(Role.CLIENT, loadedClient.role());

            assertEquals(1, work.users().findByRole(Role.MANAGER).size());

            // Verifica conto bancario
            BankAccount loadedAccount = work.bankAccounts().findByClientId(client.requireId()).orElseThrow();
            assertEquals(account.iban(), loadedAccount.iban());
            assertEquals(new BigDecimal("1000.00"), loadedAccount.balance());

            // Verifica catalogo e sconti
            Vehicle loadedVehicle = work.vehicles().findById(saleVehicle.requireId()).orElseThrow();
            assertEquals("AA100AA", loadedVehicle.licencePlate());
            assertEquals(VehicleState.SALE, loadedVehicle.state());

            var vd = work.discounts().findVehicleDiscount(saleVehicle.requireId()).orElseThrow();
            assertEquals(15, vd.value());

            work.commit();
        }
    }

    @Test
    void persistsRentRequestAndSale() {
        long clientId;
        long salesmanId;
        long vehicleId;

        try (var work = daoFactory.begin()) {
            User c = work.users().save(new it.unifi.ing.drivehub.domain.users.Client(null, "Anna", "Neri", "anna@example.com", "hash"));
            User s = work.users().save(new it.unifi.ing.drivehub.domain.users.Salesman(null, "Pietro", "Gialli", "pietro@example.com", "hash"));
            work.brands().save(new Brand("Fiat"));
            Model m = work.models().save(new Model(null, "Fiat", "500", new BigDecimal("18000.00"), 2022));
            Vehicle v = work.vehicles().save(new Vehicle(null, m.requireId(), "VIN200", "BB200BB",
                    20000, new BigDecimal("80.00"), 2022, VehicleState.RENT));

            clientId = c.requireId();
            salesmanId = s.requireId();
            vehicleId = v.requireId();

            // Salva un noleggio
            Rent rent = Rent.create(vehicleId, clientId, new BigDecimal("80.00"),
                    LocalDate.now(), LocalDate.now().plusDays(2));
            work.rentals().save(rent);

            // Salva un test drive
            TestDrive td = TestDrive.create(vehicleId, clientId, LocalDate.now().plusDays(1),
                    LocalTime.of(10, 0));
            work.testDrives().save(td);

            work.commit();
        }

        try (var work = daoFactory.begin()) {
            assertEquals(1, work.rentals().findByClientId(clientId).size());
            assertEquals(1, work.testDrives().findByClientId(clientId).size());

            // Salva una vendita
            Sale sale = Sale.record(vehicleId, salesmanId, clientId, new BigDecimal("80.00"), java.time.LocalDateTime.now());
            work.sales().save(sale);
            work.commit();
        }

        try (var work = daoFactory.begin()) {
            assertEquals(1, work.sales().findAll().size());
            work.commit();
        }
    }
}
