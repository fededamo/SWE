package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.sales.SaleOrderStatus;

import java.util.List;
import java.util.Optional;

public interface SaleOrderDao {
    SaleOrder save(SaleOrder order);
    void update(SaleOrder order);
    Optional<SaleOrder> findById(long id);
    List<SaleOrder> findByCustomer(long customerId);
    List<SaleOrder> findByStatus(SaleOrderStatus status);
    boolean claimIfUnassigned(long orderId, long salesmanId);
}
