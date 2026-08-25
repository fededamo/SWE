package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;

import java.util.List;
import java.util.Optional;

public interface VehicleModelDao {
    VehicleModel save(VehicleModel model);
    Optional<VehicleModel> findById(long id);
    Optional<VehicleModel> findByCode(String code);
    List<VehicleModel> findByBrand(long brandId);
    List<VehicleModel> findAll();
}
