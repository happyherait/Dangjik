package io.github.happyherait.dangjik.ops;

import io.github.happyherait.dangjik.common.ApiError;
import io.github.happyherait.dangjik.common.AuditLog;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** SR(서비스 요청) 티켓. 상담 문의에서 만들거나 운영자가 직접 만든다. */
@Repository
public class TicketRepository {

    public record Ticket(String ticketId, String area, String severity, String title, String description,
                        String status, String inquiryId, String createdBy, LocalDateTime createdAt) {
    }

    public record NewTicket(String area, String severity, String title, String description, String inquiryId,
                            String createdBy) {
    }

    private static final Set<String> AREAS = Set.of("MALL", "PARTNER", "CRM", "APP");
    private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH");

    private final JdbcClient jdbc;
    private final OpsFactsRepository ops;
    private final AuditLog audit;

    public TicketRepository(JdbcClient jdbc, OpsFactsRepository ops, AuditLog audit) {
        this.jdbc = jdbc;
        this.ops = ops;
        this.audit = audit;
    }

    public List<Ticket> list() {
        return jdbc.sql("""
                SELECT ticket_id, area, severity, title, description, status, inquiry_id, created_by, created_at
                  FROM sr_tickets ORDER BY created_at DESC
                """).query(Ticket.class).list();
    }

    public Optional<Ticket> find(String ticketId) {
        return jdbc.sql("""
                SELECT ticket_id, area, severity, title, description, status, inquiry_id, created_by, created_at
                  FROM sr_tickets WHERE ticket_id = :t
                """).param("t", ticketId).query(Ticket.class).optional();
    }

    @Transactional
    public Ticket create(NewTicket t) {
        if (!AREAS.contains(t.area()) || !SEVERITIES.contains(t.severity())) {
            throw new ApiError.Conflict("영역 또는 심각도가 올바르지 않습니다.");
        }
        if (t.title() == null || t.title().isBlank()) {
            throw new ApiError.Conflict("제목을 입력하세요.");
        }
        if (ops.staffRole(t.createdBy()).isEmpty()) {
            throw new ApiError.Conflict("등록되지 않은 직원입니다: " + t.createdBy());
        }
        if (t.inquiryId() != null && ops.inquiry(t.inquiryId()).isEmpty()) {
            throw new ApiError.Conflict("문의가 없습니다: " + t.inquiryId());
        }
        String id = "SR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        jdbc.sql("""
                INSERT INTO sr_tickets (ticket_id, area, severity, title, description, status, inquiry_id, created_by,
                                        created_at)
                VALUES (:id, :area, :sev, :title, :descr, 'OPEN', :inq, :by, SYSTIMESTAMP)
                """)
                .param("id", id).param("area", t.area()).param("sev", t.severity()).param("title", t.title())
                .param("descr", t.description(), Types.VARCHAR).param("inq", t.inquiryId(), Types.VARCHAR)
                .param("by", t.createdBy())
                .update();
        if (t.inquiryId() != null) {
            jdbc.sql("UPDATE cs_inquiries SET status = 'ESCALATED' WHERE inquiry_id = :q")
                    .param("q", t.inquiryId()).update();
        }
        audit.record(t.createdBy(), "TICKET_CREATED", id, t.title());
        return find(id).orElseThrow();
    }
}
