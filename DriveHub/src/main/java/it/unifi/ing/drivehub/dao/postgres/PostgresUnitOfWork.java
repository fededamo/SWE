package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.BrandDao;
import it.unifi.ing.drivehub.dao.interfaces.DiscountDao;
import it.unifi.ing.drivehub.dao.interfaces.PaymentDao;
import it.unifi.ing.drivehub.dao.interfaces.PurchaseProposalDao;
import it.unifi.ing.drivehub.dao.interfaces.RentalDao;
import it.unifi.ing.drivehub.dao.interfaces.SaleOrderDao;
import it.unifi.ing.drivehub.dao.interfaces.StockOrderDao;
import it.unifi.ing.drivehub.dao.interfaces.TestDriveDao;
import it.unifi.ing.drivehub.dao.interfaces.UnitOfWork;
import it.unifi.ing.drivehub.dao.interfaces.UserDao;
import it.unifi.ing.drivehub.dao.interfaces.VehicleDao;
import it.unifi.ing.drivehub.dao.interfaces.VehicleModelDao;

import java.sql.Connection;
import java.sql.SQLException;

final class PostgresUnitOfWork implements UnitOfWork {

    private final Connection connection;
    private final UserDao users;
    private final BrandDao brands;
    private final VehicleModelDao vehicleModels;
    private final VehicleDao vehicles;
    private final DiscountDao discounts;
    private final RentalDao rentals;
    private final TestDriveDao testDrives;
    private final SaleOrderDao saleOrders;
    private final PurchaseProposalDao purchaseProposals;
    private final PaymentDao payments;
    private final StockOrderDao stockOrders;
    private boolean completed;
    private boolean closed;

    PostgresUnitOfWork(Connection connection) {
        this.connection = connection;
        JdbcEntityLoader loader = new JdbcEntityLoader(connection);
        users = new PostgresUserDao(connection, loader);
        brands = new PostgresBrandDao(connection, loader);
        vehicleModels = new PostgresVehicleModelDao(connection, loader);
        vehicles = new PostgresVehicleDao(connection, loader);
        discounts = new PostgresDiscountDao(connection, loader);
        rentals = new PostgresRentalDao(connection, loader);
        testDrives = new PostgresTestDriveDao(connection, loader);
        saleOrders = new PostgresSaleOrderDao(connection, loader);
        purchaseProposals = new PostgresPurchaseProposalDao(connection, loader);
        payments = new PostgresPaymentDao(connection, loader);
        stockOrders = new PostgresStockOrderDao(connection, loader);
    }

    @Override public UserDao users() { ensureOpen(); return users; }
    @Override public BrandDao brands() { ensureOpen(); return brands; }
    @Override public VehicleModelDao vehicleModels() { ensureOpen(); return vehicleModels; }
    @Override public VehicleDao vehicles() { ensureOpen(); return vehicles; }
    @Override public DiscountDao discounts() { ensureOpen(); return discounts; }
    @Override public RentalDao rentals() { ensureOpen(); return rentals; }
    @Override public TestDriveDao testDrives() { ensureOpen(); return testDrives; }
    @Override public SaleOrderDao saleOrders() { ensureOpen(); return saleOrders; }
    @Override public PurchaseProposalDao purchaseProposals() { ensureOpen(); return purchaseProposals; }
    @Override public PaymentDao payments() { ensureOpen(); return payments; }
    @Override public StockOrderDao stockOrders() { ensureOpen(); return stockOrders; }

    @Override
    public void commit() {
        ensureActive();
        try {
            connection.commit();
            completed = true;
        } catch (SQLException exception) {
            rollbackAfterFailure(exception);
            throw new DatabaseException("Could not commit unit of work", exception);
        }
    }

    @Override
    public void rollback() {
        ensureActive();
        try {
            connection.rollback();
            completed = true;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not roll back unit of work", exception);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        DatabaseException pending = null;
        if (!completed) {
            try {
                connection.rollback();
            } catch (SQLException exception) {
                pending = new DatabaseException("Could not roll back uncommitted unit of work", exception);
            }
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            if (pending == null) {
                pending = new DatabaseException("Could not close unit of work", exception);
            } else {
                pending.addSuppressed(exception);
            }
        } finally {
            closed = true;
        }
        if (pending != null) {
            throw pending;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("unit of work is closed");
        }
    }

    private void ensureActive() {
        ensureOpen();
        if (completed) {
            throw new IllegalStateException("unit of work is already completed");
        }
    }

    private void rollbackAfterFailure(SQLException failure) {
        try {
            connection.rollback();
            completed = true;
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
