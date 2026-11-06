package io.github.happyherait.dangjik.ops;

import io.github.happyherait.dangjik.common.ApiError;
import io.github.happyherait.dangjik.common.AuditLog;
import io.github.happyherait.dangjik.runbook.ApprovalDecision;
import io.github.happyherait.dangjik.runbook.ApprovalRules;
import io.github.happyherait.dangjik.runbook.Change;
import io.github.happyherait.dangjik.runbook.PaymentResyncRunbook;
import io.github.happyherait.dangjik.runbook.RunbookPlan;
import io.github.happyherait.dangjik.runbook.Skip;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * 런북 제안, 승인, 실행.
 *
 * <p>제안은 누구나(AI 포함) 할 수 있지만 DB를 바꾸지 않는다. 실행은 운영 팀장의 승인으로만 일어나며,
 * 승인 순간에 사실을 다시 읽어 계획을 다시 계산하고, 승인한 계획과 같을 때만 실행한다.
 */
@Service
public class RunbookService {

    /** DB에 저장하는 제안 내용. 승인할 때 requestedOrderIds로 계획을 다시 계산한다. */
    public record StoredPlan(List<String> requestedOrderIds, String reason, List<Change> changes, List<Skip> skipped) {
    }

    public record RunView(String runId, String runbookId, String ticketId, String requestedBy, String status,
                          LocalDateTime createdAt, LocalDateTime executedAt, String planHash, StoredPlan proposal,
                          String currentPlanHash, boolean stale) {
    }

    public record Proposal(RunbookPlan plan, String runId, boolean created) {
    }

    private record RunRow(String runId, String runbookId, String ticketId, String requestedBy, String planHash,
                          String planJson, String status, LocalDateTime createdAt, LocalDateTime executedAt) {
    }

    private static final String RUN_SELECT = """
            SELECT run_id, runbook_id, ticket_id, requested_by, plan_hash, plan_json, status, created_at, executed_at
              FROM runbook_runs
            """;

    private final JdbcClient jdbc;
    private final PaymentFactsRepository facts;
    private final OpsFactsRepository ops;
    private final AuditLog audit;
    private final ObjectMapper json;

    public RunbookService(JdbcClient jdbc, PaymentFactsRepository facts, OpsFactsRepository ops, AuditLog audit,
                          ObjectMapper json) {
        this.jdbc = jdbc;
        this.facts = facts;
        this.ops = ops;
        this.audit = audit;
        this.json = json;
    }

    /** 현재 사실로 계획을 계산한다. DB는 바꾸지 않는다. */
    public RunbookPlan plan(String runbookId, List<String> orderIds) {
        if (!PaymentResyncRunbook.ID.equals(runbookId)) {
            throw new ApiError.Conflict("알 수 없는 런북입니다: " + runbookId);
        }
        try {
            return PaymentResyncRunbook.plan(orderIds, facts.orders(orderIds), facts.pgTransactions(orderIds));
        } catch (IllegalArgumentException e) {
            throw new ApiError.Conflict(e.getMessage());
        }
    }

    /**
     * 실행 제안을 승인 대기로 등록한다. 바꿀 것이 없으면 등록하지 않는다.
     * 같은 계획이 이미 승인 대기 중이면 새로 만들지 않고 그 건을 돌려준다.
     */
    @Transactional
    public Proposal propose(String runbookId, List<String> orderIds, String reason, String requestedBy,
                            String ticketId) {
        if (ops.staffRole(requestedBy).isEmpty()) {
            throw new ApiError.Conflict("등록되지 않은 직원입니다: " + requestedBy);
        }
        List<String> ids = orderIds.stream().distinct().toList();
        RunbookPlan plan = plan(runbookId, ids);
        if (plan.isEmpty()) {
            return new Proposal(plan, null, false);
        }
        String key = runbookId + ":" + plan.planHash();
        Optional<String> existing = jdbc.sql("SELECT run_id FROM runbook_runs WHERE idempotency_key = :k")
                .param("k", key).query(String.class).optional();
        if (existing.isPresent()) {
            return new Proposal(plan, existing.get(), false);
        }
        String runId = "RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        jdbc.sql("""
                INSERT INTO runbook_runs (run_id, runbook_id, ticket_id, requested_by, plan_hash, plan_json, status,
                                          idempotency_key, created_at)
                VALUES (:id, :rb, :ticket, :by, :hash, :plan, 'WAITING_APPROVAL', :key, SYSTIMESTAMP)
                """)
                .param("id", runId).param("rb", runbookId).param("ticket", ticketId, Types.VARCHAR).param("by", requestedBy)
                .param("hash", plan.planHash())
                .param("plan", json.writeValueAsString(new StoredPlan(ids, reason, plan.changes(), plan.skipped())))
                .param("key", key)
                .update();
        audit.record(requestedBy, "RUNBOOK_PROPOSED", runId,
                runbookId + " 대상 " + plan.changes().size() + "건. 사유: " + reason);
        return new Proposal(plan, runId, true);
    }

    public List<RunView> runs() {
        return jdbc.sql(RUN_SELECT + " ORDER BY created_at DESC").query(RunRow.class).list()
                .stream().map(this::view).toList();
    }

    public Optional<RunView> run(String runId) {
        return findRow(runId, false).map(this::view);
    }

    @Transactional
    public RunView approve(String runId, String approverId, String approvedHash) {
        RunRow row = waitingRow(runId);
        StoredPlan stored = read(row.planJson());
        RunbookPlan current = plan(row.runbookId(), stored.requestedOrderIds());
        String role = ops.staffRole(approverId).orElse("UNKNOWN");

        ApprovalDecision decision = ApprovalRules.decide(row.requestedBy(), approverId, role, approvedHash, current);
        if (!decision.allowed()) {
            audit.record(approverId, "APPROVAL_DENIED", runId, decision.reason());
            throw new ApiError.Conflict(decision.reason());
        }
        // 제안 당시 계획과도 같아야 한다. 승인 화면이 오래된 경우를 막는다.
        if (!current.planHash().equals(row.planHash())) {
            audit.record(approverId, "APPROVAL_DENIED", runId, "제안 후 사실이 바뀜");
            throw new ApiError.Conflict("제안 후 사실이 바뀌었습니다. 다시 제안해야 합니다.");
        }

        for (Change c : current.changes()) {
            int updated = jdbc.sql("UPDATE orders SET status = 'PAID' WHERE order_id = :o AND status = 'PAYMENT_PENDING'")
                    .param("o", c.orderId()).update();
            if (updated != 1) {
                throw new ApiError.Conflict(c.orderId() + " 주문 상태가 바뀌어 실행을 멈췄습니다.");
            }
            jdbc.sql("""
                    INSERT INTO payments (payment_id, order_id, pg_tx_id, amount, paid_at)
                    VALUES (:id, :o, :tx, :amount, SYSTIMESTAMP)
                    """)
                    .param("id", "PAY-R-" + c.pgTxId()).param("o", c.orderId()).param("tx", c.pgTxId())
                    .param("amount", c.amount())
                    .update();
            jdbc.sql("""
                    UPDATE order_items SET shipment_status = 'READY'
                     WHERE order_id = :o AND shipment_status = 'WAITING_PAYMENT'
                    """).param("o", c.orderId()).update();
            audit.record(approverId, "ORDER_PAYMENT_RESYNCED", c.orderId(),
                    "PG " + c.pgTxId() + " " + c.amount() + "원 기준으로 결제완료 처리 (" + runId + ")");
        }

        jdbc.sql("""
                INSERT INTO approvals (run_id, approver_id, decision, plan_hash, decided_at)
                VALUES (:r, :a, 'APPROVE', :h, SYSTIMESTAMP)
                """).param("r", runId).param("a", approverId).param("h", approvedHash).update();
        jdbc.sql("UPDATE runbook_runs SET status = 'EXECUTED', executed_at = SYSTIMESTAMP WHERE run_id = :r")
                .param("r", runId).update();
        if (row.ticketId() != null) {
            jdbc.sql("UPDATE sr_tickets SET status = 'FIXED' WHERE ticket_id = :t").param("t", row.ticketId()).update();
        }
        audit.record(approverId, "RUNBOOK_EXECUTED", runId, current.changes().size() + "건 실행");
        return view(findRow(runId, false).orElseThrow());
    }

    @Transactional
    public RunView reject(String runId, String approverId, String reason) {
        RunRow row = waitingRow(runId);
        if (!ApprovalRules.OPS_LEAD.equals(ops.staffRole(approverId).orElse(""))) {
            throw new ApiError.Conflict("운영 팀장(OPS_LEAD)만 반려할 수 있음");
        }
        jdbc.sql("""
                INSERT INTO approvals (run_id, approver_id, decision, plan_hash, reason, decided_at)
                VALUES (:r, :a, 'REJECT', :h, :reason, SYSTIMESTAMP)
                """).param("r", runId).param("a", approverId).param("h", row.planHash()).param("reason", reason, Types.VARCHAR)
                .update();
        jdbc.sql("UPDATE runbook_runs SET status = 'REJECTED' WHERE run_id = :r").param("r", runId).update();
        audit.record(approverId, "RUNBOOK_REJECTED", runId, reason);
        return view(findRow(runId, false).orElseThrow());
    }

    private RunRow waitingRow(String runId) {
        RunRow row = findRow(runId, true).orElseThrow(() -> new ApiError.Conflict("실행 요청이 없습니다: " + runId));
        if (!"WAITING_APPROVAL".equals(row.status())) {
            throw new ApiError.Conflict("이미 처리된 실행 요청입니다 (" + row.status() + ").");
        }
        return row;
    }

    private Optional<RunRow> findRow(String runId, boolean lock) {
        return jdbc.sql(RUN_SELECT + " WHERE run_id = :r" + (lock ? " FOR UPDATE" : ""))
                .param("r", runId).query(RunRow.class).optional();
    }

    private RunView view(RunRow row) {
        StoredPlan stored = read(row.planJson());
        String currentHash = null;
        boolean stale = false;
        if ("WAITING_APPROVAL".equals(row.status())) {
            currentHash = plan(row.runbookId(), stored.requestedOrderIds()).planHash();
            stale = !currentHash.equals(row.planHash());
        }
        return new RunView(row.runId(), row.runbookId(), row.ticketId(), row.requestedBy(), row.status(),
                row.createdAt(), row.executedAt(), row.planHash(), stored, currentHash, stale);
    }

    private StoredPlan read(String planJson) {
        return json.readValue(planJson, StoredPlan.class);
    }
}
