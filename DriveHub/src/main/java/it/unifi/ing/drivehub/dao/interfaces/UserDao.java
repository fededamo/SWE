package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    User save(User user);
    void update(User user);
    Optional<User> findById(long id);
    /** Serializes bookings for the same customer, including different vehicles. */
    Optional<User> findByIdForUpdate(long id);
    Optional<User> findByEmail(String normalizedEmail);
    Optional<User> findByFiscalCode(String normalizedFiscalCode);
    List<User> findByRole(Role role);
}
