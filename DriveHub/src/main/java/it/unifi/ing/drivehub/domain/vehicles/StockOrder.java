package it.unifi.ing.drivehub.domain.vehicles;

import it.unifi.ing.drivehub.domain.BaseEntity;
import it.unifi.ing.drivehub.domain.DomainRuleViolationException;
import it.unifi.ing.drivehub.domain.users.Role;
import it.unifi.ing.drivehub.domain.users.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class StockOrder extends BaseEntity {
    private final User manager;
    private final VehicleModel model;
    private final int quantity;
    private final BigDecimal unitCost;
    private final LocalDate placedOn;
    private StockOrderStatus status;
    private LocalDate receivedOn;

    public StockOrder(Long id, User manager, VehicleModel model, int quantity, BigDecimal unitCost,
                      LocalDate placedOn, StockOrderStatus status, LocalDate receivedOn) {
        super(id);
        this.manager = Objects.requireNonNull(manager, "manager");
        if (!manager.hasRole(Role.MANAGER)) {
            throw new IllegalArgumentException("manager must have role MANAGER");
        }
        this.model = Objects.requireNonNull(model, "model");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.quantity = quantity;
        this.unitCost = Objects.requireNonNull(unitCost, "unitCost");
        if (unitCost.signum() <= 0) {
            throw new IllegalArgumentException("unitCost must be positive");
        }
        this.placedOn = Objects.requireNonNull(placedOn, "placedOn");
        this.status = Objects.requireNonNull(status, "status");
        this.receivedOn = receivedOn;
        if (status == StockOrderStatus.RECEIVED && receivedOn == null) {
            throw new IllegalArgumentException("received stock order requires a received date");
        }
        if (receivedOn != null && receivedOn.isBefore(placedOn)) {
            throw new IllegalArgumentException("received date cannot precede placement");
        }
    }

    public static StockOrder place(User manager, VehicleModel model, int quantity,
                                   BigDecimal unitCost, LocalDate placedOn) {
        return new StockOrder(null, manager, model, quantity, unitCost, placedOn,
                StockOrderStatus.PLACED, null);
    }

    public User manager() { return manager; }
    public VehicleModel model() { return model; }
    public int quantity() { return quantity; }
    public BigDecimal unitCost() { return unitCost; }
    public LocalDate placedOn() { return placedOn; }
    public StockOrderStatus status() { return status; }
    public LocalDate receivedOn() { return receivedOn; }
    public BigDecimal totalCost() { return unitCost.multiply(BigDecimal.valueOf(quantity)); }

    public void confirm(User actor) {
        requireManager(actor);
        transition(StockOrderStatus.PLACED, StockOrderStatus.CONFIRMED);
    }

    public void receive(User actor, LocalDate date) {
        requireManager(actor);
        if (status != StockOrderStatus.CONFIRMED) {
            throw new DomainRuleViolationException("only a confirmed stock order can be received");
        }
        if (date == null || date.isBefore(placedOn)) {
            throw new IllegalArgumentException("invalid received date");
        }
        receivedOn = date;
        status = StockOrderStatus.RECEIVED;
    }

    public void cancel(User actor) {
        requireManager(actor);
        if (status != StockOrderStatus.PLACED && status != StockOrderStatus.CONFIRMED) {
            throw new DomainRuleViolationException("stock order cannot be cancelled in status " + status);
        }
        status = StockOrderStatus.CANCELLED;
    }

    private void transition(StockOrderStatus expected, StockOrderStatus next) {
        if (status != expected) {
            throw new DomainRuleViolationException("stock order transition requires " + expected);
        }
        status = next;
    }

    private void requireManager(User actor) {
        actor.requireRole(Role.MANAGER);
        if (manager.id() != null && actor.id() != null) {
            if (!manager.id().equals(actor.id())) {
                throw new DomainRuleViolationException("stock order belongs to another manager");
            }
        } else if (manager != actor) {
            throw new DomainRuleViolationException("stock order belongs to another manager");
        }
    }
}
