package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.vehicles.StockOrder;
import it.unifi.ing.drivehub.domain.vehicles.StockOrderStatus;

import java.util.List;
import java.util.Optional;

public interface StockOrderDao {
    StockOrder save(StockOrder order);
    void update(StockOrder order);
    Optional<StockOrder> findById(long id);
    List<StockOrder> findAll();
    List<StockOrder> findByStatus(StockOrderStatus status);
}
