package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.sales.Discount;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiscountDao {
    Discount save(Discount discount);
    void update(Discount discount);
    Optional<Discount> findById(long id);
    List<Discount> findAll();
    List<Discount> findActiveOn(LocalDate date);
}
