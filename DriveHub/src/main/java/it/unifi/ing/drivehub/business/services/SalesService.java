package it.unifi.ing.drivehub.business.services;

import it.unifi.ing.drivehub.business.core.TransactionRunner;
import it.unifi.ing.drivehub.business.exceptions.ConflictException;
import it.unifi.ing.drivehub.business.exceptions.EntityNotFoundException;
import it.unifi.ing.drivehub.business.strategies.PricingStrategy;
import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.dao.interfaces.UnitOfWork;
import it.unifi.ing.drivehub.domain.sales.SaleOrder;
import it.unifi.ing.drivehub.domain.sales.SaleOrderStatus;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;
import it.unifi.ing.drivehub.domain.vehicles.Vehicle;
import it.unifi.ing.drivehub.domain.vehicles.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class SalesService {
    private final TransactionRunner transactions;
    private final PricingStrategy pricing;

    public SalesService(DaoFactory daoFactory, PricingStrategy pricing) {
        this.transactions = new TransactionRunner(daoFactory);
        this.pricing = Objects.requireNonNull(pricing, "pricing");
    }

    /** UC-C-PURCHASE: reserves one available catalog vehicle at the current quoted price. */
    public SaleOrder reserveVehicle(long customerId, long vehicleId, BigDecimal requiredDeposit,
                                    LocalDate pricingDate) {
        Objects.requireNonNull(requiredDeposit, "requiredDeposit");
        return transactions.execute(unit -> reserveVehicle(unit, customerId, vehicleId, requiredDeposit, pricingDate));
    }

    SaleOrder reserveVehicle(UnitOfWork unit, long customerId, long vehicleId,
                             BigDecimal requiredDeposit, LocalDate pricingDate) {
        User customer = unit.users().findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("customer", customerId));
        customer.requireRole(Role.CUSTOMER);
        Vehicle vehicle = unit.vehicles().findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("vehicle", vehicleId));
        if (!vehicle.isAvailableForSale()) {
            throw new ConflictException("vehicle is not available for sale");
        }
        if (unit.vehicles().hasOpenBookings(vehicleId)) {
            throw new ConflictException("Il veicolo ha prenotazioni aperte");
        }
        BigDecimal total = pricing.salePrice(vehicle, unit.discounts().findActiveOn(pricingDate), pricingDate);
        BigDecimal deposit = requiredDeposit == null ? PricingService.requiredSaleDeposit(total) : requiredDeposit;
        SaleOrder order = SaleOrder.reserve(customer, vehicle, total, deposit);
        if (!unit.vehicles().updateStatusIfCurrent(vehicleId,
                VehicleStatus.AVAILABLE, VehicleStatus.RESERVED)) {
            throw new ConflictException("vehicle was reserved by another customer");
        }
        return unit.saleOrders().save(order);
    }

    public SaleOrder claimOrder(long orderId, long salesmanId) {
        return transactions.execute(unit -> {
            User salesman = unit.users().findById(salesmanId)
                    .orElseThrow(() -> new EntityNotFoundException("salesman", salesmanId));
            salesman.requireRole(Role.SALESMAN);
            if (!unit.saleOrders().claimIfUnassigned(orderId, salesmanId)) {
                throw new ConflictException("sale order was already claimed");
            }
            return unit.saleOrders().findByIdForUpdate(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("sale order", orderId));
        });
    }

    public SaleOrder completeOrder(long orderId, long salesmanId) {
        return transactions.execute(unit -> {
            SaleOrder order = unit.saleOrders().findByIdForUpdate(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("sale order", orderId));
            User salesman = unit.users().findById(salesmanId)
                    .orElseThrow(() -> new EntityNotFoundException("salesman", salesmanId));
            order.complete(salesman);
            if (!unit.vehicles().updateStatusIfCurrent(order.vehicle().requireId(),
                    VehicleStatus.RESERVED, VehicleStatus.SOLD)) {
                throw new ConflictException("vehicle sale state changed concurrently");
            }
            unit.saleOrders().update(order);
            return order;
        });
    }

    public SaleOrder cancelOrder(long orderId, long actorId) {
        return transactions.execute(unit -> {
            SaleOrder order = unit.saleOrders().findByIdForUpdate(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("sale order", orderId));
            User actor = unit.users().findById(actorId)
                    .orElseThrow(() -> new EntityNotFoundException("user", actorId));
            boolean releasesVehicle = order.status() != SaleOrderStatus.CANCELLED;
            order.cancel(actor);
            if (releasesVehicle && !unit.vehicles().updateStatusIfCurrent(order.vehicle().requireId(),
                    VehicleStatus.RESERVED, VehicleStatus.AVAILABLE)) {
                throw new ConflictException("vehicle sale state changed concurrently");
            }
            unit.saleOrders().update(order);
            return order;
        });
    }

    public List<SaleOrder> ordersForCustomer(long customerId) {
        return transactions.execute(unit -> {
            unit.users().findById(customerId)
                    .orElseThrow(() -> new EntityNotFoundException("customer", customerId)).requireRole(Role.CUSTOMER);
            return List.copyOf(unit.saleOrders().findByCustomer(customerId));
        });
    }
}
