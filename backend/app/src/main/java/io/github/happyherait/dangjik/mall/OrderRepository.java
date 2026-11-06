package io.github.happyherait.dangjik.mall;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    public record OrderItem(int lineNo, String productId, String productName, int qty, long unitPrice,
                            String shipmentStatus) {
    }

    public record Order(String orderId, String memberId, String channel, String status, long totalAmount,
                        LocalDateTime orderedAt, List<OrderItem> items) {
    }

    private record OrderRow(String orderId, String memberId, String channel, String status, long totalAmount,
                            LocalDateTime orderedAt) {
    }

    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Order> findById(String orderId) {
        return jdbc.sql("""
                SELECT order_id, member_id, channel, status, total_amount, ordered_at
                  FROM orders
                 WHERE order_id = :orderId
                """)
                .param("orderId", orderId)
                .query(OrderRow.class)
                .optional()
                .map(row -> new Order(row.orderId(), row.memberId(), row.channel(), row.status(),
                        row.totalAmount(), row.orderedAt(), items(orderId)));
    }

    private List<OrderItem> items(String orderId) {
        return jdbc.sql("""
                SELECT i.line_no, i.product_id, p.name AS product_name, i.qty, i.unit_price, i.shipment_status
                  FROM order_items i
                  JOIN products p ON p.product_id = i.product_id
                 WHERE i.order_id = :orderId
                 ORDER BY i.line_no
                """)
                .param("orderId", orderId)
                .query(OrderItem.class)
                .list();
    }
}
