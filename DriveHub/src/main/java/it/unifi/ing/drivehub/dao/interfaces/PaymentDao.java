package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.sales.Payment;
import it.unifi.ing.drivehub.domain.sales.PaymentReferenceType;

import java.util.List;
import java.util.Optional;

public interface PaymentDao {
    Payment save(Payment payment);
    void update(Payment payment);
    Optional<Payment> findById(long id);
    List<Payment> findAll();
    List<Payment> findByReference(PaymentReferenceType referenceType, long referenceId);
}
