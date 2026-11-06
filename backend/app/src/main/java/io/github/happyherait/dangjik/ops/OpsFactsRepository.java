package io.github.happyherait.dangjik.ops;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * 운영 콘솔과 AI 당직이 읽는 사실. 모두 읽기 전용이다.
 * AI 도구는 이 클래스의 조회만 쓸 수 있고, 쓰기는 RunbookService의 승인 절차를 거쳐야 한다.
 */
@Repository
public class OpsFactsRepository {

    public record OrderFact(String orderId, String memberId, String channel, String status, long totalAmount,
                            LocalDateTime orderedAt) {
    }

    public record OrderLineFact(int lineNo, String productId, String productName, int qty, long unitPrice,
                                String shipmentStatus) {
    }

    public record PaymentFact(String paymentId, String pgTxId, long amount, LocalDateTime paidAt) {
    }

    public record PgFact(String pgTxId, String orderId, String status, long amount, LocalDateTime approvedAt) {
    }

    public record WebhookLog(long logId, String pgTxId, String orderId, int httpStatus, String message,
                             LocalDateTime receivedAt) {
    }

    public record Deployment(String deployId, String area, String version, String note, String deployedBy,
                             LocalDateTime deployedAt) {
    }

    public record BatchJob(String jobId, String name, String lastStatus, String lastMessage,
                           LocalDateTime lastRunAt) {
    }

    public record StuckPayment(String orderId, String orderStatus, long orderAmount, String pgTxId,
                               long pgAmount, LocalDateTime approvedAt) {
    }

    public record ProductFact(String productId, String partnerId, String name, long price, int stock,
                              String description, String status) {
    }

    public record InquiryFact(String inquiryId, String memberId, String orderId, String channel, String title,
                              String body, String status, LocalDateTime createdAt) {
    }

    public record AppVersion(String platform, String latestVersion, String minSupportedVersion,
                             LocalDateTime updatedAt) {
    }

    public record Approval(String runId, String approverId, String decision, String reason,
                           LocalDateTime decidedAt) {
    }

    public record AuditEvent(long eventId, String actorId, String action, String target, String detail,
                             LocalDateTime createdAt) {
    }

    private final JdbcClient jdbc;

    public OpsFactsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<OrderFact> order(String orderId) {
        return jdbc.sql("""
                SELECT order_id, member_id, channel, status, total_amount, ordered_at
                  FROM orders WHERE order_id = :o
                """).param("o", orderId).query(OrderFact.class).optional();
    }

    public List<OrderLineFact> orderLines(String orderId) {
        return jdbc.sql("""
                SELECT i.line_no, i.product_id, p.name AS product_name, i.qty, i.unit_price, i.shipment_status
                  FROM order_items i JOIN products p ON p.product_id = i.product_id
                 WHERE i.order_id = :o ORDER BY i.line_no
                """).param("o", orderId).query(OrderLineFact.class).list();
    }

    public Optional<PaymentFact> payment(String orderId) {
        return jdbc.sql("SELECT payment_id, pg_tx_id, amount, paid_at FROM payments WHERE order_id = :o")
                .param("o", orderId).query(PaymentFact.class).optional();
    }

    public List<PgFact> pgTransactions(String orderId) {
        return jdbc.sql("""
                SELECT pg_tx_id, order_id, status, amount, approved_at
                  FROM pg_transactions WHERE order_id = :o ORDER BY pg_tx_id
                """).param("o", orderId).query(PgFact.class).list();
    }

    public List<WebhookLog> webhookLogs(String orderId) {
        return jdbc.sql("""
                SELECT log_id, pg_tx_id, order_id, http_status, message, received_at
                  FROM webhook_logs WHERE order_id = :o ORDER BY received_at
                """).param("o", orderId).query(WebhookLog.class).list();
    }

    public List<WebhookLog> failedWebhooks() {
        return jdbc.sql("""
                SELECT log_id, pg_tx_id, order_id, http_status, message, received_at
                  FROM webhook_logs WHERE http_status <> 200 ORDER BY received_at DESC
                """).query(WebhookLog.class).list();
    }

    public List<Deployment> deployments() {
        return jdbc.sql("""
                SELECT deploy_id, area, version, note, deployed_by, deployed_at
                  FROM deployments ORDER BY deployed_at DESC
                """).query(Deployment.class).list();
    }

    public List<BatchJob> batchJobs() {
        return jdbc.sql("SELECT job_id, name, last_status, last_message, last_run_at FROM batch_jobs ORDER BY job_id")
                .query(BatchJob.class).list();
    }

    /** PG는 승인했는데 우리 쪽은 아직 결제대기인 주문 */
    public List<StuckPayment> stuckPayments() {
        return jdbc.sql("""
                SELECT o.order_id, o.status AS order_status, o.total_amount AS order_amount,
                       t.pg_tx_id, t.amount AS pg_amount, t.approved_at
                  FROM orders o JOIN pg_transactions t ON t.order_id = o.order_id
                 WHERE o.status = 'PAYMENT_PENDING' AND t.status = 'APPROVED'
                 ORDER BY o.ordered_at
                """).query(StuckPayment.class).list();
    }

    public Optional<ProductFact> product(String productId) {
        return jdbc.sql("""
                SELECT product_id, partner_id, name, price, stock, description, status
                  FROM products WHERE product_id = :p
                """).param("p", productId).query(ProductFact.class).optional();
    }

    public Optional<InquiryFact> inquiry(String inquiryId) {
        return jdbc.sql("""
                SELECT inquiry_id, member_id, order_id, channel, title, body, status, created_at
                  FROM cs_inquiries WHERE inquiry_id = :q
                """).param("q", inquiryId).query(InquiryFact.class).optional();
    }

    public List<AppVersion> appVersions() {
        return jdbc.sql("""
                SELECT platform, latest_version, min_supported_version, updated_at
                  FROM app_versions ORDER BY platform
                """).query(AppVersion.class).list();
    }

    public List<AuditEvent> auditEvents() {
        return jdbc.sql("""
                SELECT event_id, actor_id, action, target, detail, created_at
                  FROM audit_events ORDER BY event_id DESC FETCH FIRST 200 ROWS ONLY
                """).query(AuditEvent.class).list();
    }

    /** 이 주문을 대상으로 남은 감사 로그 (주문 단위와 주문 상품 단위) */
    public List<AuditEvent> auditFor(String orderId) {
        return jdbc.sql("""
                SELECT event_id, actor_id, action, target, detail, created_at
                  FROM audit_events
                 WHERE target = :o OR target LIKE :prefix
                 ORDER BY event_id
                """).param("o", orderId).param("prefix", orderId + "#%").query(AuditEvent.class).list();
    }

    public List<Approval> approvals(String runId) {
        return jdbc.sql("""
                SELECT run_id, approver_id, decision, reason, decided_at
                  FROM approvals WHERE run_id = :r ORDER BY approval_id
                """).param("r", runId).query(Approval.class).list();
    }

    public Optional<String> staffRole(String userId) {
        return jdbc.sql("SELECT role FROM staff_users WHERE user_id = :u")
                .param("u", userId).query(String.class).optional();
    }
}
