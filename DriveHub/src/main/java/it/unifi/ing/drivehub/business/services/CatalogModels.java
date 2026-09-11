package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.dao.interfaces.UnitOfWork;
import it.unifi.ing.drivehub.domain.vehicles.Brand;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;
import java.util.Locale;

/** Shared identity lookup within the caller's transaction, for stock and acquisition. */
final class CatalogModels {
    private CatalogModels() { }

    static VehicleModel findOrCreate(UnitOfWork unit, String brandName, String modelName, int year) {
        Brand validatedBrand = Brand.create(brandName);
        String code = "ACQ-" + (validatedBrand.name() + "-" + modelName + "-" + year)
                .toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (code.length() > 64) throw new IllegalArgumentException("Nome marca/modello troppo lungo");
        Brand brand = unit.brands().findByName(validatedBrand.name())
                .orElseGet(() -> unit.brands().save(validatedBrand));
        VehicleModel candidate = VehicleModel.create(brand, code, modelName, year);
        return unit.vehicleModels().findAll().stream()
                .filter(existing -> existing.brand().requireId() == brand.requireId())
                .filter(existing -> existing.name().equals(candidate.name()) && existing.modelYear() == year)
                .findFirst().orElseGet(() -> unit.vehicleModels().save(candidate));
    }
}
