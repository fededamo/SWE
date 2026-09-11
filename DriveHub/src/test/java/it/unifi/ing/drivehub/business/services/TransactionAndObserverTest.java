package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.dao.interfaces.*;
import it.unifi.ing.drivehub.domain.observer.InventoryObserver;
import it.unifi.ing.drivehub.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionAndObserverTest {
    @Test @DisplayName("UT-TX-01: commit failure rolls back and closes without hiding the original failure")
    void commitFailurePreservesCauseAndCloses() {
        DaoFactory factory = mock(DaoFactory.class); UnitOfWork unit = mock(UnitOfWork.class);
        when(factory.begin()).thenReturn(unit);
        var original = new IllegalStateException("commit failure");
        var rollback = new IllegalStateException("rollback failure");
        doThrow(original).when(unit).commit(); doThrow(rollback).when(unit).rollback();
        assertSame(original, assertThrows(IllegalStateException.class,
                () -> new TransactionRunner(factory).execute(u -> "result")));
        assertArrayEquals(new Throwable[]{rollback}, original.getSuppressed());
        var order = inOrder(unit); order.verify(unit).commit(); order.verify(unit).rollback(); order.verify(unit).close();
    }

    @Test @DisplayName("UT-OBS-01: an uncommitted inventory change never reaches the Observer")
    void rolledBackEventIsNeverDelivered() {
        DaoFactory factory = mock(DaoFactory.class); UnitOfWork unit = mock(UnitOfWork.class);
        UserDao users = mock(UserDao.class); VehicleDao vehicles = mock(VehicleDao.class);
        when(factory.begin()).thenReturn(unit); when(unit.users()).thenReturn(users); when(unit.vehicles()).thenReturn(vehicles);
        when(users.findById(2L)).thenReturn(Optional.of(TestFixtures.salesman(2L)));
        when(vehicles.findByIdForUpdate(3L)).thenReturn(Optional.of(TestFixtures.rentalVehicle(3L, "UT100AA")));
        doThrow(new IllegalStateException("commit failure")).when(unit).commit();
        InventoryService service = new InventoryService(factory); InventoryObserver observer = mock(InventoryObserver.class);
        service.addObserver(observer);
        assertThrows(IllegalStateException.class, () -> service.sendToMaintenance(2L, 3L));
        verifyNoInteractions(observer); verify(unit).rollback(); verify(unit).close();
    }

    @Test @DisplayName("UT-OBS-02: notification happens after commit and close")
    void notificationFollowsCommit() {
        DaoFactory factory = mock(DaoFactory.class); UnitOfWork unit = mock(UnitOfWork.class);
        UserDao users = mock(UserDao.class); VehicleDao vehicles = mock(VehicleDao.class);
        when(factory.begin()).thenReturn(unit); when(unit.users()).thenReturn(users); when(unit.vehicles()).thenReturn(vehicles);
        when(users.findById(2L)).thenReturn(Optional.of(TestFixtures.salesman(2L)));
        when(vehicles.findByIdForUpdate(3L)).thenReturn(Optional.of(TestFixtures.rentalVehicle(3L, "UT100AA")));
        InventoryService service = new InventoryService(factory); InventoryObserver observer = mock(InventoryObserver.class);
        service.addObserver(observer); service.sendToMaintenance(2L, 3L);
        var order = inOrder(unit, observer); order.verify(unit).commit(); order.verify(unit).close(); order.verify(observer).onInventoryEvent(any());
    }
}
