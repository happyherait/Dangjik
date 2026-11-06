package io.github.happyherait.dangjik.runbook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApprovalRulesTest {

    private final RunbookPlan plan = PaymentResyncRunbook.plan(
            List.of("ORD-1"),
            Map.of("ORD-1", new OrderSnapshot("ORD-1", "PAYMENT_PENDING", 39_000)),
            List.of(new PgTransaction("PG-1", "ORD-1", "APPROVED", 39_000)));

    @Test
    void 다른_사람인_운영팀장이_같은_계획을_승인하면_허용() {
        assertTrue(ApprovalRules.decide("ops.kim", "lead.park", "OPS_LEAD", plan.planHash(), plan).allowed());
    }

    @Test
    void 요청자_본인_승인은_거부() {
        assertFalse(ApprovalRules.decide("lead.park", "lead.park", "OPS_LEAD", plan.planHash(), plan).allowed());
    }

    @Test
    void 운영팀장이_아니면_거부() {
        assertFalse(ApprovalRules.decide("ops.kim", "cs.lee", "CS_AGENT", plan.planHash(), plan).allowed());
    }

    @Test
    void 승인_후_계획이_바뀌면_거부() {
        assertFalse(ApprovalRules.decide("ops.kim", "lead.park", "OPS_LEAD", "old-hash", plan).allowed());
    }

    @Test
    void 변경이_없는_계획은_거부() {
        RunbookPlan empty = PaymentResyncRunbook.plan(List.of("ORD-404"), Map.of(), List.of());
        assertFalse(ApprovalRules.decide("ops.kim", "lead.park", "OPS_LEAD", empty.planHash(), empty).allowed());
    }
}
