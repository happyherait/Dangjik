package io.github.happyherait.dangjik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.happyherait.dangjik.common.ApiError;
import io.github.happyherait.dangjik.crm.CrmRepository;
import io.github.happyherait.dangjik.partner.PartnerRepository;
import io.github.happyherait.dangjik.partner.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PartnerCrmIT {

    @Autowired
    PartnerRepository partners;

    @Autowired
    ShipmentService shipments;

    @Autowired
    CrmRepository crm;

    @Autowired
    JdbcClient jdbc;

    @Test
    void 입점사_대시보드는_자기_상품만_센다() {
        PartnerRepository.Dashboard d = partners.dashboard("P-001");

        assertEquals(2, d.onSale());
        assertEquals(1, d.soldOut());
        assertEquals(1, d.readyToShip());
        assertEquals(1, d.waitingPayment());
        assertEquals(0, d.openClaims());
        assertEquals("2026-10", d.latestSettlement().period());
    }

    @Test
    void 입점사_주문_목록에는_다른_입점사_상품이_없다() {
        assertTrue(partners.orderLines("P-003").stream()
                .allMatch(l -> l.productId().equals("PRD-0007") || l.productId().equals("PRD-0008")));
        assertEquals(1, partners.claims("P-002").size());
        assertTrue(partners.claims("P-001").isEmpty());
    }

    @Test
    void 없는_입점사의_정산은_비어_있다() {
        assertTrue(partners.settlements("P-999").isEmpty());
        assertNull(partners.dashboard("P-999").latestSettlement());
    }

    @Test
    @Transactional
    void 결제가_확인된_자기_상품만_출고할_수_있다() {
        ApiError.Conflict unpaid = assertThrows(ApiError.Conflict.class,
                () -> shipments.ship("P-001", "ORD-20261105-0007", 1, "p001.han"));
        assertTrue(unpaid.getMessage().contains("결제가 확인되지 않은"));

        assertThrows(ApiError.Conflict.class, () -> shipments.ship("P-002", "ORD-20261105-0004", 1, "p002.yoon"));

        shipments.ship("P-001", "ORD-20261105-0004", 1, "p001.han");
        assertEquals("SHIPPING", jdbc.sql("SELECT status FROM orders WHERE order_id = 'ORD-20261105-0004'")
                .query(String.class).single());
        assertEquals(1, jdbc.sql("SELECT COUNT(*) FROM audit_events WHERE target = 'ORD-20261105-0004#1'")
                .query(Integer.class).single());

        assertThrows(ApiError.Conflict.class, () -> shipments.ship("P-001", "ORD-20261105-0004", 1, "p001.han"));
    }

    @Test
    void 회원_360은_주문과_문의를_함께_보여준다() {
        CrmRepository.Member360 m = crm.member360("M-1001").orElseThrow();

        assertEquals(2, m.orders().size());
        assertEquals(39000, m.totalSpent());
        assertEquals("INQ-0001", m.inquiries().getFirst().inquiryId());
        assertTrue(crm.member360("M-9999").isEmpty());
    }

    @Test
    void 세그먼트는_등급_순서로_나온다() {
        assertEquals("VIP", crm.segments().getFirst().grade());
        assertEquals(5, crm.segments().stream().mapToInt(CrmRepository.Segment::members).sum());
    }
}
