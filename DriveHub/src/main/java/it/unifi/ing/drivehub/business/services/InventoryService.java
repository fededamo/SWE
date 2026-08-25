package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.domain.observer.InventoryObserver;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Brand;
import it.unifi.ing.drivehub.domain.vehicles.StockOrder;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleModel;
import it.unifi.ing.drivehub.domain.vehicles.VehiclePurpose;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InventoryService {
    private final TransactionRunner transactions;
    private final Clock clock;
    private final List<InventoryObserver> observers = new CopyOnWriteArrayList<>();

    public InventoryService(DaoFactory daoFactory, Clock clock) {
        this.transactions = new TransactionRunner(daoFactory);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public InventoryService(DaoFactory daoFactory) {
        this(daoFactory, Clock.systemDefaultZone());
    }

    public void addObserver(InventoryObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    public void removeObserver(InventoryObserver observer) {
        observers.remove(observer);
    }

    public Brand addBrand(long salesmanId, String name) {
        return transactions.execute(unit -> {
            requireSalesman(unit.users().findById(salesmanId), salesmanId);
            if (unit.brands().findByName(name).isPresent()) {
                throw new ConflictException("brand already exists");
            }
            return unit.brands().save(Brand.create(name));
        });
    }

    public VehicleModel addModel(long salesmanId, long brandId, String code, String name, int modelYear) {
        return transactions.execute(unit -> {
            requireSalesman(unit.users().findById(salesmanId), salesmanId);
            if (unit.vehicleModels().findByCode(code).isPresent()) {
                throw new ConflictException("vehicle model code already exists");
            }
            Brand brand = unit.brands().findById(brandId)
                    .orElseThrow(() -> new EntityNotFoundException("brand", brandId));
            return unit.vehicleModels().save(VehicleModel.create(brand, code, name, modelYear));
        });
    }

    public Vehicle addVehicle(long salesmanId, long modelId, String plate, VehiclePurpose purpose,
                              BigDecimal salePrice, BigDecimal dailyRentalRate, long mileage) {
        if (purpose == VehiclePurpose.ACQUISITION_REQUEST) {
            throw new IllegalArgumentException("acquisition requests are created by the proposal workflow");
        }
        return transactions.execute(unit -> {
            requireSalesman(unit.users().findById(salesmanId), salesmanId);
            VehicleModel model = unit.vehicleModels().findById(modelId)
                    .orElseThrow(() -> new EntityNotFoundException("vehicle model", modelId));
            String normalizedPlate = Vehicle.normalizePlate(plate);
            if (unit.vehicles().findByPlate(normalizedPlate).isPresent()) {
                throw new ConflictException("plate is already registered");
            }
            Vehicle vehicle = Vehicle.create(normalizedPlate, model, purpose,
                    salePrice, dailyRentalRate, mileage);
            attachObservers(vehicle);
            return unit.vehicles().save(vehicle);
        });
    }

    public Vehicle sendToMaintenance(long salesmanId, long vehicleId) {
        return changeMaintenance(salesmanId, vehicleId, true);
    }

    public Vehicle returnFromMaintenance(long salesmanId, long vehicleId) {
        return changeMaintenance(salesmanId, vehicleId, false);
    }

    public StockOrder placeStockOrder(long managerId, long modelId, int quantity, BigDecimal unitCost) {
        return transactions.execute(unit -> {
            User manager = requireManager(unit.users().findById(managerId), managerId);
            VehicleModel model = unit.vehicleModels().findById(modelId)
                    .orElseThrow(() -> new EntityNotFoundException("vehicle model", modelId));
            return unit.stockOrders().save(StockOrder.place(manager, model, quantity, unitCost,
                    LocalDate.now(clock)));
        });
    }

    public StockOrder confirmStockOrder(long managerId, long orderId) {
        return updateStockOrder(managerId, orderId, false);
    }

    public StockOrder receiveStockOrder(long managerId, long orderId) {
        return updateStockOrder(managerId, orderId, true);
    }

    public List<Vehicle> inventory() {
        return transactions.execute(unit -> List.copyOf(unit.vehicles().findAll()));
    }

    public List<StockOrder> stockOrders() {
        return transactions.execute(unit -> List.copyOf(unit.stockOrders().findAll()));
    }

    private Vehicle changeMaintenance(long salesmanId, long vehicleId, boolean start) {
        return transactions.execute(unit -> {
            requireSalesman(unit.users().findById(salesmanId), salesmanId);
            Vehicle vehicle = unit.vehicles().findById(vehicleId)
                    .orElseThrow(() -> new EntityNotFoundException("vehicle", vehicleId));
            attachObservers(vehicle);
            if (start) {
                vehicle.sendToMaintenance();
            } else {
                vehicle.returnFromMaintenance();
            }
            unit.vehicles().update(vehicle);
            return vehicle;
        });
    }

    private StockOrder updateStockOrder(long managerId, long orderId, boolean receive) {
        return transactions.execute(unit -> {
            User manager = requireManager(unit.users().findById(managerId), managerId);
            StockOrder order = unit.stockOrders().findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("stock order", orderId));
            if (receive) {
                order.receive(manager, LocalDate.now(clock));
            } else {
                order.confirm(manager);
            }
            unit.stockOrders().update(order);
            return order;
        });
    }

    private void attachObservers(Vehicle vehicle) {
        observers.forEach(vehicle::subscribe);
    }

    private static User requireSalesman(java.util.Optional<User> value, long id) {
        User user = value.orElseThrow(() -> new EntityNotFoundException("salesman", id));
        user.requireRole(Role.SALESMAN);
        return user;
    }

    private static User requireManager(java.util.Optional<User> value, long id) {
        User user = value.orElseThrow(() -> new EntityNotFoundException("manager", id));
        user.requireRole(Role.MANAGER);
        return user;
    }
}
