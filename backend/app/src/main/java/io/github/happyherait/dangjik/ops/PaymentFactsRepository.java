package io.github.happyherait.dangjik.ops;

import io.github.happyherait.dangjik.runbook.OrderSnapshot;
import io.github.happyherait.dangjik.runbook.PgTransaction;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** 런북이 판단에 쓰는 사실(주문 상태, PG 원장)을 DB에서 읽는다. */
@Repository
public class PaymentFactsRepository {

    private final JdbcClient jdbc;

    public PaymentFactsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, OrderSnapshot> orders(List<String> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql("SELECT order_id, status, total_amount AS amount FROM orders WHERE order_id IN (:ids)")
                .param("ids", orderIds)
                .query(OrderSnapshot.class)
                .list()
                .stream()
                .collect(Collectors.toMap(OrderSnapshot::orderId, Function.identity()));
    }

    public List<PgTransaction> pgTransactions(List<String> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT pg_tx_id, order_id, status, amount FROM pg_transactions WHERE order_id IN (:ids)")
                .param("ids", orderIds)
                .query(PgTransaction.class)
                .list();
    }
}
