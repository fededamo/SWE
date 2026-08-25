package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.vehicles.Brand;

import java.util.List;
import java.util.Optional;

public interface BrandDao {
    Brand save(Brand brand);
    Optional<Brand> findById(long id);
    Optional<Brand> findByName(String name);
    List<Brand> findAll();
}
