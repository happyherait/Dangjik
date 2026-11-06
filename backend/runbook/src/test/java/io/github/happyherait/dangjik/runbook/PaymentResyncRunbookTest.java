package io.github.happyherait.dangjik.runbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PaymentResyncRunbookTest {

    private static final Map<String, OrderSnapshot> ORDERS = Map.of(
            "ORD-1", new OrderSnapshot("ORD-1", "PAYMENT_PENDING", 39_000),
            "ORD-2", new OrderSnapshot("ORD-2", "PAID", 12_000),
            "ORD-3", new OrderSnapshot("ORD-3", "PAYMENT_PENDING", 50_000),
            "ORD-4", new OrderSnapshot("ORD-4", "PAYMENT_PENDING", 20_000),
            "ORD-5", new OrderSnapshot("ORD-5", "PAYMENT_PENDING", 8_000));

    private static final List<PgTransaction> PG = List.of(
            new PgTransaction("PG-1", "ORD-1", "APPROVED", 39_000),
            new PgTransaction("PG-2", "ORD-2", "APPROVED", 12_000),
            new PgTransaction("PG-3", "ORD-3", "APPROVED", 5_000),
            new PgTransaction("PG-5a", "ORD-5", "APPROVED", 8_000),
            new PgTransaction("PG-5b", "ORD-5", "APPROVED", 8_000));

    @Test
    void 웹훅이_누락된_주문만_결제완료로_바꾼다() {
        RunbookPlan plan = PaymentResyncRunbook.plan(List.of("ORD-1"), ORDERS, PG);

        assertEquals(List.of(new Change("ORD-1", "PAYMENT_PENDING", "PAID", "PG-1", 39_000)), plan.changes());
        assertTrue(plan.skipped().isEmpty());
    }

    @Test
    void 사실과_맞지_않는_요청은_이유와_함께_제외한다() {
        RunbookPlan plan = PaymentResyncRunbook.plan(
                List.of("ORD-2", "ORD-3", "ORD-4", "ORD-5", "ORD-404"), ORDERS, PG);

        assertTrue(plan.isEmpty());
        assertEquals(5, plan.skipped().size());
        assertTrue(plan.skipped().get(0).reason().contains("결제대기 상태가 아님"));
        assertTrue(plan.skipped().get(1).reason().contains("금액 불일치"));
        assertTrue(plan.skipped().get(2).reason().contains("PG 승인 거래가 없음"));
        assertTrue(plan.skipped().get(3).reason().contains("2건 이상"));
        assertTrue(plan.skipped().get(4).reason().contains("주문이 없음"));
    }

    @Test
    void 한번에_처리할_수_있는_건수에_상한이_있다() {
        List<String> tooMany = IntStream.rangeClosed(1, PaymentResyncRunbook.MAX_TARGETS + 1)
                .mapToObj(i -> "ORD-X" + i).toList();

        assertThrows(IllegalArgumentException.class, () -> PaymentResyncRunbook.plan(tooMany, ORDERS, PG));
        assertThrows(IllegalArgumentException.class, () -> PaymentResyncRunbook.plan(List.of(), ORDERS, PG));
    }

    @Test
    void 같은_사실이면_같은_해시_사실이_바뀌면_다른_해시() {
        RunbookPlan first = PaymentResyncRunbook.plan(List.of("ORD-1", "ORD-1"), ORDERS, PG);
        RunbookPlan again = PaymentResyncRunbook.plan(List.of("ORD-1"), ORDERS, PG);
        assertEquals(first.planHash(), again.planHash());

        List<PgTransaction> changedAmount = List.of(new PgTransaction("PG-1", "ORD-1", "APPROVED", 39_000),
                new PgTransaction("PG-9", "ORD-3", "APPROVED", 50_000));
        RunbookPlan widened = PaymentResyncRunbook.plan(List.of("ORD-1", "ORD-3"), ORDERS, changedAmount);
        assertNotEquals(first.planHash(), widened.planHash());
    }
}
