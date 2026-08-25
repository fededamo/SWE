package it.unifi.ing.drivehub.dao.interfaces;

import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PurchaseProposalDao {
    PurchaseProposal save(PurchaseProposal proposal);
    void update(PurchaseProposal proposal);
    Optional<PurchaseProposal> findById(long id);
    List<PurchaseProposal> findByCustomer(long customerId);
    List<PurchaseProposal> findBySalesman(long salesmanId);
    List<PurchaseProposal> findRequested();
    List<PurchaseProposal> findAwaitingManagerDecision();

    /** Atomically creates the first salesman offer for a requested proposal. */
    boolean submitOfferIfRequested(long proposalId, long salesmanId, BigDecimal amount, String terms);

    /** Atomically records the first manager decision for an OFFERED proposal. */
    boolean decideIfOffered(long proposalId, long managerId, boolean approved, String reason);
}
