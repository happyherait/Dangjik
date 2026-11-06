package io.github.happyherait.dangjik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.happyherait.dangjik.mall.OrderRepository;
import io.github.happyherait.dangjik.mall.ProductRepository;
import io.github.happyherait.dangjik.ops.PaymentFactsRepository;
import io.github.happyherait.dangjik.runbook.Change;
import io.github.happyherait.dangjik.runbook.PaymentResyncRunbook;
import io.github.happyherait.dangjik.runbook.RunbookPlan;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 실제 Oracle에 마이그레이션과 시드가 적용되고, 장애 시나리오가 재현되는지 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SeedDataIT {

    @Autowired
    ProductRepository products;

    @Autowired
    OrderRepository orders;

    @Autowired
    PaymentFactsRepository facts;

    @Test
    void 시드_상품이_조회된다() {
        assertEquals(10, products.findAll().size());
    }

    @Test
    void 상품_하나를_번호로_조회한다() {
        assertEquals("세라믹 머그 2P", products.findById("PRD-0006").orElseThrow().name());
        assertTrue(products.findById("PRD-9999").isEmpty());
    }

    @Test
    void 장애_주문은_결제대기로_남아_있다() {
        OrderRepository.Order order = orders.findById("ORD-20261105-0007").orElseThrow();

        assertEquals("PAYMENT_PENDING", order.status());
        assertEquals("APP", order.channel());
        assertEquals("제철 사과 3kg", order.items().getFirst().productName());
    }

    @Test
    void 런북은_DB_사실로_판단해_장애_주문만_대상으로_삼는다() {
        List<String> requested = List.of("ORD-20261105-0007", "ORD-20261105-0005", "ORD-20261105-0004");

        RunbookPlan plan = PaymentResyncRunbook.plan(requested, facts.orders(requested),
                facts.pgTransactions(requested));

        assertEquals(List.of(new Change("ORD-20261105-0007", "PAYMENT_PENDING", "PAID", "PGTX-88007", 39000)),
                plan.changes());
        assertEquals(2, plan.skipped().size());
        assertTrue(plan.skipped().stream().anyMatch(s -> s.orderId().equals("ORD-20261105-0005")));
    }
}
