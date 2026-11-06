package io.github.happyherait.dangjik.partner;

import io.github.happyherait.dangjik.common.ApiError;
import io.github.happyherait.dangjik.common.AuditLog;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 입점사의 출고 처리. 결제가 확인된 주문의 자기 상품만 출고할 수 있다.
 * 결제대기 주문은 출고할 수 없어서, 웹훅 누락 장애가 나면 입점사도 출고를 못 한다.
 */
@Service
public class ShipmentService {

    private final JdbcClient jdbc;
    private final AuditLog audit;

    public ShipmentService(JdbcClient jdbc, AuditLog audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @Transactional
    public void ship(String partnerId, String orderId, int lineNo, String actorId) {
        record Line(String partnerId, String shipmentStatus, String orderStatus) {
        }
        Line line = jdbc.sql("""
                SELECT p.partner_id, i.shipment_status, o.status AS order_status
                  FROM order_items i
                  JOIN orders o   ON o.order_id = i.order_id
                  JOIN products p ON p.product_id = i.product_id
                 WHERE i.order_id = :o AND i.line_no = :l
                   FOR UPDATE OF i.shipment_status, o.status
                """).param("o", orderId).param("l", lineNo).query(Line.class).optional()
                .orElseThrow(() -> new ApiError.Conflict("주문 상품이 없습니다."));

        if (!line.partnerId().equals(partnerId)) {
            throw new ApiError.Conflict("다른 입점사의 상품은 출고할 수 없습니다.");
        }
        if (!"PAID".equals(line.orderStatus()) && !"SHIPPING".equals(line.orderStatus())) {
            throw new ApiError.Conflict("결제가 확인되지 않은 주문은 출고할 수 없습니다.");
        }
        if (!"READY".equals(line.shipmentStatus())) {
            throw new ApiError.Conflict("출고 준비 상태가 아닙니다.");
        }

        jdbc.sql("UPDATE order_items SET shipment_status = 'SHIPPING' WHERE order_id = :o AND line_no = :l")
                .param("o", orderId).param("l", lineNo).update();
        jdbc.sql("UPDATE orders SET status = 'SHIPPING' WHERE order_id = :o AND status = 'PAID'")
                .param("o", orderId).update();
        audit.record(actorId, "SHIP_ORDER_LINE", orderId + "#" + lineNo, "입점사 " + partnerId + " 출고");
    }
}
