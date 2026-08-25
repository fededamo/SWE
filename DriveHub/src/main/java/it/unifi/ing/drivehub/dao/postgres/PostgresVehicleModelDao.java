package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.VehicleModelDao;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class PostgresVehicleModelDao extends AbstractPostgresDao implements VehicleModelDao {

    PostgresVehicleModelDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public VehicleModel save(VehicleModel model) {
        if (model.id() != null) {
            throw new IllegalArgumentException("vehicle model is already persistent");
        }
        long id = execute("Could not save vehicle model", () -> JdbcSupport.insert(connection, """
                INSERT INTO vehicle_models(brand_id, code, name, model_year) VALUES (?, ?, ?, ?)
                """, statement -> {
            statement.setLong(1, model.brand().requireId());
            statement.setString(2, model.code());
            statement.setString(3, model.name());
            statement.setInt(4, model.modelYear());
        }));
        model.assignId(id);
        return model;
    }

    @Override
    public Optional<VehicleModel> findById(long id) {
        return loader.vehicleModel(id);
    }

    @Override
    public Optional<VehicleModel> findByCode(String code) {
        return findOne("SELECT id FROM vehicle_models WHERE code = ?", statement ->
                statement.setString(1, code.trim().toUpperCase(Locale.ROOT)), loader::vehicleModel,
                "Could not find vehicle model by code");
    }

    @Override
    public List<VehicleModel> findByBrand(long brandId) {
        return findMany("SELECT id FROM vehicle_models WHERE brand_id = ? ORDER BY name, model_year, id",
                statement -> statement.setLong(1, brandId), loader::vehicleModel,
                "Could not list vehicle models by brand");
    }

    @Override
    public List<VehicleModel> findAll() {
        return findMany("SELECT id FROM vehicle_models ORDER BY code, id", statement -> { },
                loader::vehicleModel, "Could not list vehicle models");
    }
}
