package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.PaymentGateway;
import it.unifi.ing.drivehub.business.core.PaymentGatewayResult;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.dao.interfaces.PaymentDao;
import it.unifi.ing.drivehub.dao.interfaces.RentalDao;
import it.unifi.ing.drivehub.dao.interfaces.UnitOfWork;
import it.unifi.ing.drivehub.dao.interfaces.UserDao;
import it.unifi.ing.drivehub.domain.rentals.Rental;
import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentMethod;
import it.unifi.ing.drivehub.domain.sales.PaymentReferenceType;
import it.unifi.ing.drivehub.domain.sales.PaymentStatus;
import it.unifi.ing.drivehub.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private UnitOfWork unit;
    private UserDao users;
    private RentalDao rentals;
    private PaymentDao payments;
    private PaymentGateway gateway;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        DaoFactory factory = mock(DaoFactory.class);
        unit = mock(UnitOfWork.class);
        users = mock(UserDao.class);
        rentals = mock(RentalDao.class);
        payments = mock(PaymentDao.class);
        gateway = mock(PaymentGateway.class);
        when(factory.begin()).thenReturn(unit);
        when(unit.users()).thenReturn(users);
        when(unit.rentals()).thenReturn(rentals);
        when(unit.payments()).thenReturn(payments);
        Clock fixed = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneOffset.UTC);
        service = new PaymentService(factory, gateway, fixed);
    }

    @Test
    @DisplayName("UC-C-RENT-PAY: approved gateway result persists completed rental payment")
    void approvedRentalPayment() {
        Rental rental = Rental.request(TestFixtures.customer(1L),
                TestFixtures.rentalVehicle(30L, "AB123CD"), LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 1, 4), new BigDecimal("300.00"));
        rental.assignId(50L);
        when(users.findById(1L)).thenReturn(Optional.of(TestFixtures.customer(1L)));
        when(rentals.findByIdForUpdate(50L)).thenReturn(Optional.of(rental));
        when(payments.findByReference(PaymentReferenceType.RENTAL, 50L)).thenReturn(List.of());
        when(payments.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0); payment.assignId(60L); return payment;
        });
        when(gateway.charge(new BigDecimal("300.00"), PaymentMethod.CARD))
                .thenReturn(PaymentGatewayResult.approved("psp-123"));

        Payment result = service.payRental(1L, 50L, PaymentMethod.CARD);

        assertEquals(PaymentStatus.COMPLETED, result.status());
        assertEquals("psp-123", result.processorReference());
        verify(payments).update(result);
        verify(unit).commit();
    }
}
