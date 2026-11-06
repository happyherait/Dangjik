package io.github.happyherait.dangjik.partner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** 파트너 포털 조회. 모든 조회는 입점사 번호로 범위를 제한한다. */
@Repository
public class PartnerRepository {

    public record Partner(String partnerId, String name, String status, double commissionRate) {
    }

    public record Dashboard(int onSale, int soldOut, int readyToShip, int waitingPayment, int openClaims,
                            Settlement latestSettlement) {
    }

    public record PartnerProduct(String productId, String name, String category, long price, int stock,
                                 String status) {
    }

    public record PartnerOrderLine(String orderId, int lineNo, String productId, String productName, int qty,
                                   long unitPrice, String shipmentStatus, String orderStatus,
                                   LocalDateTime orderedAt, String memberName) {
    }

    public record PartnerClaim(String claimId, String orderId, String claimType, String status, String reason,
                               LocalDateTime requestedAt) {
    }

    public record Settlement(String settlementId, String period, long grossAmount, long commission,
                             long netAmount, String status) {
    }

    private final JdbcClient jdbc;

    public PartnerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Partner> partners() {
        return jdbc.sql("SELECT partner_id, name, status, commission_rate FROM partners ORDER BY partner_id")
                .query(Partner.class).list();
    }

    public Optional<Partner> partner(String partnerId) {
        return jdbc.sql("SELECT partner_id, name, status, commission_rate FROM partners WHERE partner_id = :p")
                .param("p", partnerId).query(Partner.class).optional();
    }

    public Dashboard dashboard(String partnerId) {
        List<Settlement> settlements = settlements(partnerId);
        return new Dashboard(
                count("SELECT COUNT(*) FROM products WHERE partner_id = :p AND status = 'ON_SALE' AND stock > 0",
                        partnerId),
                count("SELECT COUNT(*) FROM products WHERE partner_id = :p AND (status = 'SOLD_OUT' OR stock = 0)",
                        partnerId),
                count("""
                        SELECT COUNT(*) FROM order_items i JOIN products p ON p.product_id = i.product_id
                         WHERE p.partner_id = :p AND i.shipment_status = 'READY'
                        """, partnerId),
                count("""
                        SELECT COUNT(*) FROM order_items i JOIN products p ON p.product_id = i.product_id
                         WHERE p.partner_id = :p AND i.shipment_status = 'WAITING_PAYMENT'
                        """, partnerId),
                claims(partnerId).stream().filter(c -> !"DONE".equals(c.status())).toList().size(),
                settlements.isEmpty() ? null : settlements.getFirst());
    }

    public List<PartnerProduct> products(String partnerId) {
        return jdbc.sql("""
                SELECT product_id, name, category, price, stock, status
                  FROM products WHERE partner_id = :p ORDER BY product_id
                """).param("p", partnerId).query(PartnerProduct.class).list();
    }

    public List<PartnerOrderLine> orderLines(String partnerId) {
        return jdbc.sql("""
                SELECT o.order_id, i.line_no, i.product_id, p.name AS product_name, i.qty, i.unit_price,
                       i.shipment_status, o.status AS order_status, o.ordered_at, m.name AS member_name
                  FROM order_items i
                  JOIN orders o   ON o.order_id = i.order_id
                  JOIN products p ON p.product_id = i.product_id
                  JOIN members m  ON m.member_id = o.member_id
                 WHERE p.partner_id = :p
                 ORDER BY o.ordered_at DESC, i.line_no
                """).param("p", partnerId).query(PartnerOrderLine.class).list();
    }

    public List<PartnerClaim> claims(String partnerId) {
        return jdbc.sql("""
                SELECT c.claim_id, c.order_id, c.claim_type, c.status, c.reason, c.requested_at
                  FROM claims c
                 WHERE EXISTS (SELECT 1 FROM order_items i JOIN products p ON p.product_id = i.product_id
                                WHERE i.order_id = c.order_id AND p.partner_id = :p)
                 ORDER BY c.requested_at DESC
                """).param("p", partnerId).query(PartnerClaim.class).list();
    }

    public List<Settlement> settlements(String partnerId) {
        return jdbc.sql("""
                SELECT settlement_id, period, gross_amount, commission, net_amount, status
                  FROM settlements WHERE partner_id = :p ORDER BY period DESC
                """).param("p", partnerId).query(Settlement.class).list();
    }

    private int count(String sql, String partnerId) {
        return jdbc.sql(sql).param("p", partnerId).query(Integer.class).single();
    }
}
