package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.strategies.PricingStrategy;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.dao.interfaces.DiscountDao;
import it.unifi.ing.drivehub.dao.interfaces.RentalDao;
import it.unifi.ing.drivehub.dao.interfaces.UnitOfWork;
import it.unifi.ing.drivehub.dao.interfaces.UserDao;
import it.unifi.ing.drivehub.dao.interfaces.VehicleDao;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.rentals.RentalStatus;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RentalServiceTest {
    private UnitOfWork unit;
    private UserDao users;
    private VehicleDao vehicles;
    private RentalDao rentals;
    private DiscountDao discounts;
    private PricingStrategy pricing;
    private RentalService service;

    @BeforeEach
    void setUp() {
        DaoFactory factory = mock(DaoFactory.class);
        unit = mock(UnitOfWork.class);
        users = mock(UserDao.class);
        vehicles = mock(VehicleDao.class);
        rentals = mock(RentalDao.class);
        discounts = mock(DiscountDao.class);
        pricing = mock(PricingStrategy.class);
        when(factory.begin()).thenReturn(unit);
        when(unit.users()).thenReturn(users);
        when(unit.vehicles()).thenReturn(vehicles);
        when(unit.rentals()).thenReturn(rentals);
        when(unit.discounts()).thenReturn(discounts);
        service = new RentalService(factory, pricing);
    }

    @Test
    @DisplayName("UC-C-RENT-SVC: request is priced and persisted with no salesman")
    void requestRentalIsUnassigned() {
        User customer = TestFixtures.customer(1L);
        Vehicle vehicle = TestFixtures.rentalVehicle(30L, "AB123CD");
        LocalDate start = LocalDate.of(2027, 2, 1);
        LocalDate end = start.plusDays(3);
        when(users.findById(1L)).thenReturn(Optional.of(customer));
        when(vehicles.findById(30L)).thenReturn(Optional.of(vehicle));
        when(rentals.findAll()).thenReturn(List.of());
        when(discounts.findActiveOn(start)).thenReturn(List.of());
        when(pricing.rentalPrice(eq(vehicle), eq(3L), anyList(), eq(start)))
                .thenReturn(new BigDecimal("300.00"));
        when(rentals.save(any())).thenAnswer(invocation -> {
            Rental rental = invocation.getArgument(0);
            rental.assignId(50L);
            return rental;
        });

        Rental created = service.requestRental(1L, 30L, start, end);

        assertEquals(RentalStatus.REQUESTED, created.status());
        assertNull(created.salesman());
        assertEquals(new BigDecimal("300.00"), created.totalPrice());
        verify(unit).commit();
    }

    @Test
    @DisplayName("UC-S-RENT-RACE: failed atomic claim reports ownership conflict")
    void failedAtomicClaimRollsBack() {
        when(users.findById(2L)).thenReturn(Optional.of(TestFixtures.salesman(2L)));
        when(rentals.claimIfUnassigned(50L, 2L)).thenReturn(false);

        assertThrows(ConflictException.class, () -> service.claimRental(50L, 2L));

        verify(unit).rollback();
        verify(rentals, never()).update(any());
    }
}
