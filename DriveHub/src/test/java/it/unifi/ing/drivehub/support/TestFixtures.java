package it.unifi.ing.drivehub.support;

import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Brand;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;

import java.math.BigDecimal;

public final class TestFixtures {
    private TestFixtures() { }

    public static User customer(long id) {
        return user(id, "RSSMRA80A01H501U", "Maria", "Rossi", "maria" + id + "@test.it",
                Role.CUSTOMER, null, true);
    }

    public static User salesman(long id) {
        return user(id, "VRDLGI80A01H501X", "Luigi", "Verdi", "luigi" + id + "@test.it",
                Role.SALESMAN, null, true);
    }

    public static User manager(long id) {
        return user(id, "BNCLCU80A01H501Z", "Luca", "Bianchi", "luca" + id + "@test.it",
                Role.MANAGER, null, true);
    }

    public static User user(long id, String fiscalCode, String firstName, String lastName,
                            String email, Role role, Long managerId, boolean active) {
        return new User(id, fiscalCode, firstName, lastName, email, "+39 055 123456",
                "encoded-password", role, managerId, active);
    }

    public static Brand brand() {
        return new Brand(10L, "Alfa Romeo");
    }

    public static VehicleModel model() {
        return new VehicleModel(20L, brand(), "GIULIA-2025", "Giulia", 2025);
    }

    public static Vehicle saleVehicle(long id, String plate) {
        return new Vehicle(id, plate, model(), VehiclePurpose.FOR_SALE,
                new BigDecimal("40000.00"), null, 12_000, it.unifi.ing.drivehub.domain.vehicles.VehicleStatus.AVAILABLE);
    }

    public static Vehicle rentalVehicle(long id, String plate) {
        return new Vehicle(id, plate, model(), VehiclePurpose.RENTAL,
                null, new BigDecimal("100.00"), 22_000,
                it.unifi.ing.drivehub.domain.vehicles.VehicleStatus.AVAILABLE);
    }

    public static Vehicle acquisitionVehicle(long id, String plate) {
        return new Vehicle(id, plate, model(), VehiclePurpose.ACQUISITION_REQUEST,
                null, null, 90_000, it.unifi.ing.drivehub.domain.vehicles.VehicleStatus.AVAILABLE);
    }
}
