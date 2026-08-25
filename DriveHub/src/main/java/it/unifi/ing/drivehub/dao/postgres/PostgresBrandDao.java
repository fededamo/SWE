package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.BrandDao;
import it.unifi.ing.drivehub.domain.vehicles.Brand;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

final class PostgresBrandDao extends AbstractPostgresDao implements BrandDao {

    PostgresBrandDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public Brand save(Brand brand) {
        if (brand.id() != null) {
            throw new IllegalArgumentException("brand is already persistent");
        }
        long id = execute("Could not save brand", () -> JdbcSupport.insert(connection,
                "INSERT INTO brands(name) VALUES (?)", statement -> statement.setString(1, brand.name())));
        brand.assignId(id);
        return brand;
    }

    @Override
    public Optional<Brand> findById(long id) {
        return loader.brand(id);
    }

    @Override
    public Optional<Brand> findByName(String name) {
        return findOne("SELECT id FROM brands WHERE name = ?", statement -> statement.setString(1, name.trim()),
                loader::brand, "Could not find brand by name");
    }

    @Override
    public List<Brand> findAll() {
        return findMany("SELECT id FROM brands ORDER BY name, id", statement -> { }, loader::brand,
                "Could not list brands");
    }
}
