package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.vehicles.Model;

import java.util.List;
import java.util.Optional;

public interface ModelDao {
    Model save(Model model);
    Optional<Model> findById(long id);
    Optional<Model> findByName(String name);
    List<Model> findByBrand(String brandName);
    List<Model> findAll();
    void delete(long modelId);
    boolean hasVehicles(long modelId);
}
