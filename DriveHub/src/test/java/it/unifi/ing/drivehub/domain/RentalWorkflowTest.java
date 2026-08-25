package it.unifi.ing.drivehub.domain;

import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.rentals.RentalStatus;
import it.unifi.ing.drivehub.domain.rentals.TestDrive;
import it.unifi.ing.drivehub.domain.rentals.TestDriveStatus;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RentalWorkflowTest {
    @Test
    @DisplayName("UC-C-RENT-01: a new rental is unassigned until a salesman claims it")
    void rentalStartsUnassigned() {
        Rental rental = rental();

        assertNull(rental.salesman());
        assertEquals(RentalStatus.REQUESTED, rental.status());
    }

    @Test
    @DisplayName("UC-S-RENT-02: first salesman claim establishes exclusive ownership")
    void claimIsExclusive() {
        Rental rental = rental();
        User first = TestFixtures.salesman(2L);
        User second = TestFixtures.user(4L, "NREFNC80A01H501P", "Franco", "Neri",
                "franco@test.it", it.unifi.ing.drivehub.domain.users.Role.SALESMAN, null, true);

        rental.claim(first);

        assertTrue(rental.isOwnedBy(first));
        assertThrows(DomainRuleViolationException.class, () -> rental.claim(second));
        assertThrows(DomainRuleViolationException.class, () -> rental.confirm(second));
        assertEquals(RentalStatus.ASSIGNED, rental.status());
    }

    @Test
    @DisplayName("UC-S-RENT-03: assigned salesman controls the complete rental state machine")
    void rentalHappyPath() {
        Rental rental = rental();
        User salesman = TestFixtures.salesman(2L);

        rental.claim(salesman);
        rental.confirm(salesman);
        rental.start(salesman);
        rental.complete(salesman);

        assertEquals(RentalStatus.COMPLETED, rental.status());
        assertThrows(DomainRuleViolationException.class, () -> rental.cancel(salesman));
    }

    @Test
    @DisplayName("UC-C-TEST-01: test drive follows request, confirmation, execution, completion")
    void testDriveWorkflow() {
        TestDrive booking = TestDrive.request(TestFixtures.customer(1L),
                TestFixtures.saleVehicle(30L, "AB123CD"), LocalDateTime.now().plusDays(2),
                Duration.ofMinutes(45));
        User salesman = TestFixtures.salesman(2L);

        booking.confirm(salesman);
        booking.start(salesman);
        booking.complete(salesman);

        assertEquals(TestDriveStatus.COMPLETED, booking.status());
    }

    private static Rental rental() {
        return Rental.request(TestFixtures.customer(1L), TestFixtures.rentalVehicle(30L, "AB123CD"),
                LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 13), new BigDecimal("300"));
    }
}
