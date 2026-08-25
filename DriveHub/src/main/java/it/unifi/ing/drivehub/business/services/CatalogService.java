package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.vehicles.Brand;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;

import java.util.List;

public final class CatalogService {
    private final TransactionRunner transactions;

    public CatalogService(DaoFactory daoFactory) {
        this.transactions = new TransactionRunner(daoFactory);
    }

    public List<Vehicle> availableForSale() {
        return transactions.execute(unit -> List.copyOf(unit.vehicles().findAvailableForSale()));
    }

    public List<Vehicle> availableForRental() {
        return transactions.execute(unit -> List.copyOf(unit.vehicles().findAvailableForRental()));
    }

    public Vehicle vehicle(long vehicleId) {
        return transactions.execute(unit -> unit.vehicles().findById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("vehicle", vehicleId)));
    }

    public List<Brand> brands() {
        return transactions.execute(unit -> List.copyOf(unit.brands().findAll()));
    }

    public List<VehicleModel> models() {
        return transactions.execute(unit -> List.copyOf(unit.vehicleModels().findAll()));
    }
}
