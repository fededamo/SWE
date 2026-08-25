package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.dao.interfaces.*;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposalStatus;
import it.unifi.ing.drivehub.domain.vehicles.Brand;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;
import it.unifi.ing.drivehub.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PurchaseProposalServiceTest {
    private UnitOfWork unit;
    private UserDao users;
    private BrandDao brands;
    private VehicleModelDao models;
    private VehicleDao vehicles;
    private PurchaseProposalDao proposals;
    private PurchaseProposalService service;

    @BeforeEach
    void setUp() {
        DaoFactory factory = mock(DaoFactory.class);
        unit = mock(UnitOfWork.class);
        users = mock(UserDao.class);
        brands = mock(BrandDao.class);
        models = mock(VehicleModelDao.class);
        vehicles = mock(VehicleDao.class);
        proposals = mock(PurchaseProposalDao.class);
        when(factory.begin()).thenReturn(unit);
        when(unit.users()).thenReturn(users);
        when(unit.brands()).thenReturn(brands);
        when(unit.vehicleModels()).thenReturn(models);
        when(unit.vehicles()).thenReturn(vehicles);
        when(unit.purchaseProposals()).thenReturn(proposals);
        Clock fixed = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"), ZoneOffset.UTC);
        service = new PurchaseProposalService(factory, fixed);
    }

    @Test
    @DisplayName("UC-C-SELL-UI: customer can submit a previously unknown brand/model transactionally")
    void requestCreatesMissingBrandAndModel() {
        when(users.findById(1L)).thenReturn(Optional.of(TestFixtures.customer(1L)));
        when(brands.findByName("Lancia")).thenReturn(Optional.empty());
        when(models.findByCode("ACQ-LANCIA-DELTA-2014")).thenReturn(Optional.empty());
        when(vehicles.findByPlate("AB123CD")).thenReturn(Optional.empty());
        when(brands.save(any())).thenAnswer(invocation -> {
            Brand brand = invocation.getArgument(0); brand.assignId(10L); return brand;
        });
        when(models.save(any())).thenAnswer(invocation -> {
            VehicleModel model = invocation.getArgument(0); model.assignId(20L); return model;
        });
        when(vehicles.save(any())).thenAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0); vehicle.assignId(30L); return vehicle;
        });
        when(proposals.save(any())).thenAnswer(invocation -> {
            PurchaseProposal proposal = invocation.getArgument(0); proposal.assignId(40L); return proposal;
        });

        PurchaseProposal result = service.request(1L, "Lancia", "Delta", 2014,
                "AB 123 CD", 88_000, new BigDecimal("8000"), "Tagliandi documentati");

        assertEquals(PurchaseProposalStatus.REQUESTED, result.status());
        assertEquals(VehiclePurpose.ACQUISITION_REQUEST, result.vehicle().purpose());
        assertEquals("AB123CD", result.vehicle().plate());
        assertEquals("ACQ-LANCIA-DELTA-2014", result.vehicle().model().code());
        verify(unit).commit();
    }

    @Test
    @DisplayName("UC-M-PROPOSAL-RACE: manager decision uses atomic OFFERED compare-and-set")
    void managerDecisionIsAtomic() {
        when(users.findById(3L)).thenReturn(Optional.of(TestFixtures.manager(3L)));
        when(proposals.decideIfOffered(40L, 3L, true, "approved")).thenReturn(false);

        assertThrows(ConflictException.class, () -> service.approve(40L, 3L, "approved"));

        verify(unit).rollback();
        verify(proposals, never()).update(any());
    }
}
