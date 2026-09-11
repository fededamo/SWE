package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.rentals.Rental;

import java.util.List;
import java.util.Optional;

public interface RentalDao {
    Rental save(Rental rental);
    void update(Rental rental);
    Optional<Rental> findById(long id);

    /** Locks the row until commit/rollback, then reads its current persisted state. */
    Optional<Rental> findByIdForUpdate(long id);
    List<Rental> findAll();
    List<Rental> findByCustomer(long customerId);
    List<Rental> findBySalesman(long salesmanId);
    List<Rental> findUnassigned();

    /** Atomically changes REQUESTED/unassigned to ASSIGNED for exactly one salesman. */
    boolean claimIfUnassigned(long rentalId, long salesmanId);
}
