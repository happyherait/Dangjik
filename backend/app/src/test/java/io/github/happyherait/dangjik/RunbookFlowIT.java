package io.github.happyherait.dangjik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.happyherait.dangjik.common.ApiError;
import io.github.happyherait.dangjik.ops.RunbookService;
import io.github.happyherait.dangjik.ops.TicketRepository;
import io.github.happyherait.dangjik.ops.TimelineService;
import io.github.happyherait.dangjik.partner.ShipmentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** 제안 → 승인 → 실행 흐름. 각 테스트는 끝나면 되돌려서 다른 테스트의 시드 데이터를 지킨다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class RunbookFlowIT {

    static final String STUCK = "ORD-20261105-0007";

    @Autowired
    RunbookService runbooks;

    @Autowired
    TicketRepository tickets;

    @Autowired
    ShipmentService shipments;

    @Autowired
    TimelineService timeline;

    @Autowired
    JdbcClient jdbc;

    @Test
    void 제안은_DB를_바꾸지_않고_같은_계획은_한_번만_등록된다() {
        RunbookService.Proposal first = runbooks.propose("PAYMENT_RESYNC", List.of(STUCK), "웹훅 누락", "ops.kim", null);
        RunbookService.Proposal again = runbooks.propose("PAYMENT_RESYNC", List.of(STUCK, STUCK), "다시", "ops.kim", null);

        assertTrue(first.created());
        assertFalse(again.created());
        assertEquals(first.runId(), again.runId());
        assertEquals("PAYMENT_PENDING", orderStatus(STUCK));
        assertEquals("WAITING_APPROVAL", runbooks.run(first.runId()).orElseThrow().status());
    }

    @Test
    void 바꿀_것이_없는_제안은_등록하지_않는다() {
        RunbookService.Proposal p = runbooks.propose("PAYMENT_RESYNC", List.of("ORD-20261105-0005"), "실패 건", "ops.kim", null);

        assertNull(p.runId());
        assertTrue(p.plan().isEmpty());
        assertTrue(p.plan().skipped().getFirst().reason().contains("PG 승인 거래가 없음"));
    }

    @Test
    void 승인_규칙을_어기면_실행되지_않는다() {
        String runId = runbooks.propose("PAYMENT_RESYNC", List.of(STUCK), "웹훅 누락", "ops.kim", null).runId();
        String hash = runbooks.run(runId).orElseThrow().planHash();

        assertThrows(ApiError.Conflict.class, () -> runbooks.approve(runId, "ops.kim", hash));
        assertThrows(ApiError.Conflict.class, () -> runbooks.approve(runId, "cs.lee", hash));
        assertThrows(ApiError.Conflict.class, () -> runbooks.approve(runId, "lead.park", "wrong-hash"));
        assertEquals("PAYMENT_PENDING", orderStatus(STUCK));
    }

    @Test
    void 팀장이_승인하면_결제완료로_바뀌고_입점사가_출고할_수_있다() {
        TicketRepository.Ticket ticket = tickets.create(new TicketRepository.NewTicket(
                "APP", "HIGH", "결제 후 결제대기 문의", null, "INQ-0001", "cs.lee"));
        String runId = runbooks.propose("PAYMENT_RESYNC", List.of(STUCK), "웹훅 누락", "ops.kim", ticket.ticketId()).runId();
        String hash = runbooks.run(runId).orElseThrow().planHash();

        assertThrows(ApiError.Conflict.class, () -> shipments.ship("P-001", STUCK, 1, "p001.han"));

        RunbookService.RunView done = runbooks.approve(runId, "lead.park", hash);

        assertEquals("EXECUTED", done.status());
        assertNotNull(done.executedAt());
        assertEquals("PAID", orderStatus(STUCK));
        assertEquals("PGTX-88007", jdbc.sql("SELECT pg_tx_id FROM payments WHERE order_id = :o")
                .param("o", STUCK).query(String.class).single());
        assertEquals("FIXED", tickets.find(ticket.ticketId()).orElseThrow().status());
        assertThrows(ApiError.Conflict.class, () -> runbooks.approve(runId, "lead.park", hash));

        shipments.ship("P-001", STUCK, 1, "p001.han");
        assertEquals("SHIPPING", orderStatus(STUCK));
        assertTrue(timeline.events().stream().anyMatch(e -> e.title().startsWith("RUNBOOK_EXECUTED")));
    }

    @Test
    void 제안_뒤_사실이_바뀌면_승인해도_실행되지_않는다() {
        String runId = runbooks.propose("PAYMENT_RESYNC", List.of(STUCK), "웹훅 누락", "ops.kim", null).runId();
        String hash = runbooks.run(runId).orElseThrow().planHash();

        // 승인 전에 누군가 PG 금액을 바꿔 놓은 상황
        jdbc.sql("UPDATE pg_transactions SET amount = 1 WHERE order_id = :o").param("o", STUCK).update();

        assertTrue(runbooks.run(runId).orElseThrow().stale());
        assertThrows(ApiError.Conflict.class, () -> runbooks.approve(runId, "lead.park", hash));
        assertEquals("PAYMENT_PENDING", orderStatus(STUCK));
    }

    @Test
    void 팀장은_반려할_수_있다() {
        String runId = runbooks.propose("PAYMENT_RESYNC", List.of(STUCK), "웹훅 누락", "ops.kim", null).runId();

        assertThrows(ApiError.Conflict.class, () -> runbooks.reject(runId, "ops.kim", "본인 반려"));
        assertEquals("REJECTED", runbooks.reject(runId, "lead.park", "PG사 확인 후 진행").status());
    }

    private String orderStatus(String orderId) {
        return jdbc.sql("SELECT status FROM orders WHERE order_id = :o").param("o", orderId).query(String.class).single();
    }
}
