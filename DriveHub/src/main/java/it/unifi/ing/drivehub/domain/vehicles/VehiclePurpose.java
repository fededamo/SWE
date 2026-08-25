package it.unifi.ing.drivehub.domain.vehicles;

/** Business destination of a vehicle record. */
public enum VehiclePurpose {
    FOR_SALE(true, false),
    RENTAL(false, true),
    ACQUISITION_REQUEST(false, false);

    private final boolean sale;
    private final boolean rental;

    VehiclePurpose(boolean sale, boolean rental) {
        this.sale = sale;
        this.rental = rental;
    }

    public boolean supportsSale() {
        return sale;
    }

    public boolean supportsRental() {
        return rental;
    }
}
