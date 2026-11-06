package io.github.happyherait.dangjik.runbook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 런북 "결제 상태 재동기화".
 *
 * <p>PG 승인 알림(웹훅)이 누락되어 결제대기로 남은 주문을, PG 원장의 승인 거래와 대조해 결제완료로 바꾼다.
 * AI가 대상 주문을 제안하더라도 실제 대상은 이 클래스가 사실(주문, PG 원장)로 다시 판단한다.
 */
public final class PaymentResyncRunbook {

    public static final String ID = "PAYMENT_RESYNC";
    public static final String PAYMENT_PENDING = "PAYMENT_PENDING";
    public static final String PAID = "PAID";
    public static final String PG_APPROVED = "APPROVED";
    public static final int MAX_TARGETS = 20;

    private PaymentResyncRunbook() {
    }

    public static RunbookPlan plan(List<String> requestedOrderIds,
                                   Map<String, OrderSnapshot> orders,
                                   List<PgTransaction> pgTransactions) {
        LinkedHashSet<String> requested = new LinkedHashSet<>(requestedOrderIds);
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("대상 주문이 없습니다.");
        }
        if (requested.size() > MAX_TARGETS) {
            throw new IllegalArgumentException("한 번에 " + MAX_TARGETS + "건까지만 처리할 수 있습니다.");
        }

        List<Change> changes = new ArrayList<>();
        List<Skip> skipped = new ArrayList<>();

        for (String orderId : requested) {
            OrderSnapshot order = orders.get(orderId);
            if (order == null) {
                skipped.add(new Skip(orderId, "주문이 없음"));
                continue;
            }
            if (!PAYMENT_PENDING.equals(order.status())) {
                skipped.add(new Skip(orderId, "결제대기 상태가 아님(" + order.status() + ")"));
                continue;
            }
            List<PgTransaction> approved = pgTransactions.stream()
                    .filter(t -> orderId.equals(t.orderId()) && PG_APPROVED.equals(t.status()))
                    .toList();
            if (approved.isEmpty()) {
                skipped.add(new Skip(orderId, "PG 승인 거래가 없음"));
                continue;
            }
            if (approved.size() > 1) {
                skipped.add(new Skip(orderId, "PG 승인 거래가 2건 이상이라 수동 확인 필요"));
                continue;
            }
            PgTransaction tx = approved.getFirst();
            if (tx.amount() != order.amount()) {
                skipped.add(new Skip(orderId, "금액 불일치(주문 " + order.amount() + ", PG " + tx.amount() + ")"));
                continue;
            }
            changes.add(new Change(orderId, PAYMENT_PENDING, PAID, tx.pgTxId(), tx.amount()));
        }

        changes.sort(Comparator.comparing(Change::orderId));
        return new RunbookPlan(ID, changes, skipped, PlanHasher.hash(ID, changes));
    }
}
