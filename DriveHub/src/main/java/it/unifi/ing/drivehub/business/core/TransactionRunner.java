package it.unifi.ing.drivehub.business.core;

import it.unifi.ing.drivehub.dao.interfaces.DaoFactory;
import it.unifi.ing.drivehub.dao.interfaces.UnitOfWork;

import java.util.Objects;
import java.util.function.Function;

public final class TransactionRunner {
    private static final ThreadLocal<UnitOfWork> CURRENT_UNIT = new ThreadLocal<>();
    private final DaoFactory daoFactory;

    public TransactionRunner(DaoFactory daoFactory) {
        this.daoFactory = Objects.requireNonNull(daoFactory, "daoFactory");
    }

    public <T> T execute(Function<UnitOfWork, T> work) {
        Objects.requireNonNull(work, "work");

        UnitOfWork current = CURRENT_UNIT.get();
        if (current != null) {
            return work.apply(current);
        }

        try (UnitOfWork unit = daoFactory.begin()) {
            CURRENT_UNIT.set(unit);
            try {
                T result = work.apply(unit);
                unit.commit();
                return result;
            } catch (RuntimeException | Error failure) {
                try {
                    unit.rollback();
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            } finally {
                CURRENT_UNIT.remove();
            }
        }
    }
}
