package it.unifi.ing.drivehub.dao.interfaces;

/** A transaction-scoped set of DAO ports. Closing without commit must roll back. */
public interface UnitOfWork extends AutoCloseable {
    UserDao users();
    BrandDao brands();
    VehicleModelDao vehicleModels();
    VehicleDao vehicles();
    DiscountDao discounts();
    RentalDao rentals();
    TestDriveDao testDrives();
    SaleOrderDao saleOrders();
    PurchaseProposalDao purchaseProposals();
    PaymentDao payments();
    StockOrderDao stockOrders();

    void commit();
    void rollback();

    @Override
    void close();
}
