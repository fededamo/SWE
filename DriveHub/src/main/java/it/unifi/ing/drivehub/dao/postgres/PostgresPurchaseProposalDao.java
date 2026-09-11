package it.unifi.ing.drivehub.dao.postgres;

import it.unifi.ing.drivehub.dao.interfaces.PurchaseProposalDao;
import it.unifi.ing.drivehub.domain.sales.PurchaseProposal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Objects;
import java.util.List;
import java.util.Optional;

final class PostgresPurchaseProposalDao extends AbstractPostgresDao implements PurchaseProposalDao {

    PostgresPurchaseProposalDao(Connection connection, JdbcEntityLoader loader) {
        super(connection, loader);
    }

    @Override
    public PurchaseProposal save(PurchaseProposal proposal) {
        if (proposal.id() != null) {
            throw new IllegalArgumentException("purchase proposal is already persistent");
        }
        long id = execute("Could not save purchase proposal", () -> JdbcSupport.insert(connection, """
                INSERT INTO purchase_proposals(
                    customer_id, vehicle_id, requested_amount, customer_notes, requested_at,
                    salesman_id, offered_amount, offer_terms, manager_id, decision_reason, status, decided_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, statement -> bindProposal(statement, proposal, false)));
        proposal.assignId(id);
        return proposal;
    }

    @Override
    public void update(PurchaseProposal proposal) {
        execute("Could not update purchase proposal", () -> JdbcSupport.updateExactlyOne(connection, """
                UPDATE purchase_proposals
                SET customer_id = ?, vehicle_id = ?, requested_amount = ?, customer_notes = ?, requested_at = ?,
                    salesman_id = ?, offered_amount = ?, offer_terms = ?, manager_id = ?, decision_reason = ?,
                    status = ?, decided_at = ?
                WHERE id = ?
                """, statement -> bindProposal(statement, proposal, true)));
    }

    @Override
    public Optional<PurchaseProposal> findById(long id) {
        return loader.purchaseProposal(id);
    }

    @Override
    public List<PurchaseProposal> findByCustomer(long customerId) {
        return findMany("""
                SELECT id FROM purchase_proposals WHERE customer_id = ? ORDER BY requested_at DESC, id
                """, statement -> statement.setLong(1, customerId), loader::purchaseProposal,
                "Could not list purchase proposals by customer");
    }

    @Override
    public List<PurchaseProposal> findBySalesman(long salesmanId) {
        return findMany("""
                SELECT id FROM purchase_proposals WHERE salesman_id = ? ORDER BY requested_at DESC, id
                """, statement -> statement.setLong(1, salesmanId), loader::purchaseProposal,
                "Could not list purchase proposals by salesman");
    }

    @Override
    public List<PurchaseProposal> findRequested() {
        return findMany("""
                SELECT id FROM purchase_proposals WHERE status = 'REQUESTED' ORDER BY requested_at, id
                """, statement -> { }, loader::purchaseProposal, "Could not list requested proposals");
    }

    @Override
    public List<PurchaseProposal> findAwaitingManagerDecision() {
        return findMany("""
                SELECT id FROM purchase_proposals WHERE status = 'OFFERED' ORDER BY requested_at, id
                """, statement -> { }, loader::purchaseProposal,
                "Could not list proposals awaiting manager decision");
    }

    @Override
    public boolean submitOfferIfRequested(long proposalId, long salesmanId, BigDecimal amount, String terms) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("offer amount must be positive");
        }
        if (terms == null || terms.isBlank()) {
            throw new IllegalArgumentException("offer terms must not be blank");
        }
        return execute("Could not atomically submit purchase offer", () -> JdbcSupport.update(connection, """
                UPDATE purchase_proposals
                SET salesman_id = ?, offered_amount = ?, offer_terms = ?, status = 'OFFERED'
                WHERE id = ? AND status = 'REQUESTED' AND salesman_id IS NULL
                  AND EXISTS (
                    SELECT 1 FROM users WHERE id = ? AND role = 'SALESMAN' AND active = TRUE
                  )
                """, statement -> {
            statement.setLong(1, salesmanId);
            statement.setBigDecimal(2, amount);
            statement.setString(3, terms.trim());
            statement.setLong(4, proposalId);
            statement.setLong(5, salesmanId);
        }) == 1);
    }

    @Override
    public boolean decideIfOffered(
            long proposalId, long managerId, boolean approved, String reason, Instant decidedAt) {
        Objects.requireNonNull(decidedAt, "decidedAt");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("decision reason must not be blank");
        }
        String decision = approved ? "APPROVED" : "REJECTED";
        return execute("Could not atomically decide purchase proposal", () -> JdbcSupport.update(connection, """
                UPDATE purchase_proposals
                SET manager_id = ?, decision_reason = ?, status = ?, decided_at = ?
                WHERE id = ? AND status = 'OFFERED' AND manager_id IS NULL
                  AND EXISTS (
                    SELECT 1 FROM users WHERE id = ? AND role = 'MANAGER' AND active = TRUE
                  )
                """, statement -> {
            statement.setLong(1, managerId);
            statement.setString(2, reason.trim());
            statement.setString(3, decision);
            statement.setTimestamp(4, Timestamp.from(decidedAt));
            statement.setLong(5, proposalId);
            statement.setLong(6, managerId);
        }) == 1);
    }

    private static void bindProposal(
            java.sql.PreparedStatement statement, PurchaseProposal proposal, boolean includeId)
            throws java.sql.SQLException {
        statement.setLong(1, proposal.customer().requireId());
        statement.setLong(2, proposal.vehicle().requireId());
        statement.setBigDecimal(3, proposal.requestedAmount());
        statement.setString(4, proposal.customerNotes());
        statement.setTimestamp(5, Timestamp.from(proposal.requestedAt()));
        JdbcSupport.setNullableLong(statement, 6,
                proposal.salesman() == null ? null : proposal.salesman().requireId());
        if (proposal.offeredAmount() == null) {
            statement.setNull(7, Types.NUMERIC);
        } else {
            statement.setBigDecimal(7, proposal.offeredAmount());
        }
        statement.setString(8, proposal.offerTerms());
        JdbcSupport.setNullableLong(statement, 9,
                proposal.manager() == null ? null : proposal.manager().requireId());
        statement.setString(10, proposal.decisionReason());
        statement.setString(11, proposal.status().name());
        statement.setTimestamp(12, proposal.decidedAt() == null ? null : Timestamp.from(proposal.decidedAt()));
        if (includeId) {
            statement.setLong(13, proposal.requireId());
        }
    }
}
