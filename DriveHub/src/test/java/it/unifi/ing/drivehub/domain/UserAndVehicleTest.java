package it.unifi.ing.drivehub.domain;

import it.unifi.ing.drivehub.domain.observer.InventoryEvent;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;
import it.unifi.ing.drivehub.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class UserAndVehicleTest {
    @Test
    @DisplayName("UC-REG-01: registration preserves ER identity fields and derives display name")
    void registrationPreservesFields() {
        User user = User.register("rssmra80a01h501u", " Maria ", " Rossi ",
                "MARIA@EXAMPLE.IT", "+39 055 123456", "hash", Role.CUSTOMER, null);

        assertEquals("RSSMRA80A01H501U", user.fiscalCode());
        assertEquals("Maria", user.firstName());
        assertEquals("Rossi", user.lastName());
        assertEquals("Maria Rossi", user.displayName());
        assertEquals("maria@example.it", user.email());
    }

    @Test
    @DisplayName("ER-USER-01: only a salesman may reference a manager")
    void onlySalesmanMayHaveManager() {
        assertThrows(IllegalArgumentException.class, () -> new User(1L,
                "RSSMRA80A01H501U", "Maria", "Rossi", "maria@test.it", "055123456",
                "hash", Role.CUSTOMER, 3L, true));

        User salesman = TestFixtures.salesman(2L);
        salesman.assignManager(TestFixtures.manager(3L));
        assertEquals(3L, salesman.managerId());
    }

    @Test
    @DisplayName("ER-HISTORY-01: inactive users remain rehydratable for historical aggregates")
    void inactiveUsersRemainRehydratable() {
        User inactiveCustomer = TestFixtures.user(1L, "RSSMRA80A01H501U", "Maria", "Rossi",
                "maria@test.it", Role.CUSTOMER, null, false);

        it.unifi.ing.drivehub.domain.rentals.Rental historical = new it.unifi.ing.drivehub.domain.rentals.Rental(
                50L, inactiveCustomer, TestFixtures.salesman(2L),
                TestFixtures.rentalVehicle(30L, "AB123CD"), java.time.LocalDate.of(2025, 1, 1),
                java.time.LocalDate.of(2025, 1, 2), BigDecimal.valueOf(100),
                it.unifi.ing.drivehub.domain.rentals.RentalStatus.COMPLETED);

        assertEquals(50L, historical.id());
        assertFalse(historical.customer().active());
    }

    @Test
    @DisplayName("INV-STATE-01: vehicle emits an Observer event for a guarded transition")
    void vehicleTransitionNotifiesObserver() {
        Vehicle vehicle = TestFixtures.saleVehicle(30L, "AB123CD");
        ArrayList<InventoryEvent> events = new ArrayList<>();
        vehicle.subscribe(events::add);

        vehicle.reserveForSale();

        assertEquals(VehicleStatus.RESERVED, vehicle.status());
        assertEquals(1, events.size());
        assertEquals("AB123CD", events.getFirst().plate());
        assertEquals(VehicleStatus.AVAILABLE, events.getFirst().previousStatus());
        assertEquals(VehicleStatus.RESERVED, events.getFirst().currentStatus());
    }

    @Test
    @DisplayName("INV-STATE-02: invalid vehicle transitions leave state unchanged")
    void invalidTransitionLeavesStateUnchanged() {
        Vehicle vehicle = TestFixtures.rentalVehicle(31L, "CD456EF");

        assertThrows(DomainRuleViolationException.class, vehicle::markSold);
        assertEquals(VehicleStatus.AVAILABLE, vehicle.status());
    }

    @Test
    @DisplayName("RF-PRICE-01: only a manager can update the price applicable to the purpose")
    void managerUpdatesPrices() {
        Vehicle vehicle = TestFixtures.saleVehicle(32L, "EF789GH");

        vehicle.updateSalePrice(TestFixtures.manager(3L), new BigDecimal("38500"));
        assertEquals(new BigDecimal("38500"), vehicle.salePrice());
        assertThrows(DomainRuleViolationException.class,
                () -> vehicle.updateDailyRentalRate(TestFixtures.manager(3L), BigDecimal.TEN));
        assertThrows(DomainRuleViolationException.class,
                () -> vehicle.updateSalePrice(TestFixtures.salesman(2L), BigDecimal.TEN));
    }
}
