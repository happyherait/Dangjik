package io.github.happyherait.dangjik.runbook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** shared/golden/runbook-cases.json 을 공개 데모(TypeScript)와 함께 쓴다. */
class GoldenCasesTest {

    @Test
    void 골든_케이스와_같은_계획과_해시를_만든다() {
        JsonNode root = new ObjectMapper().readTree(new File("../../shared/golden/runbook-cases.json"));
        Map<String, OrderSnapshot> orders = new LinkedHashMap<>();
        root.get("orders").forEach(o -> orders.put(o.get("orderId").asString(),
                new OrderSnapshot(o.get("orderId").asString(), o.get("status").asString(), o.get("amount").asLong())));
        List<PgTransaction> pg = new ArrayList<>();
        root.get("pgTransactions").forEach(t -> pg.add(new PgTransaction(t.get("pgTxId").asString(),
                t.get("orderId").asString(), t.get("status").asString(), t.get("amount").asLong())));

        for (JsonNode c : root.get("cases")) {
            List<String> requested = new ArrayList<>();
            c.get("requested").forEach(r -> requested.add(r.asString()));
            RunbookPlan plan = PaymentResyncRunbook.plan(requested, orders, pg);

            String name = c.get("name").asString();
            assertEquals(texts(c.get("changedOrders")), plan.changes().stream().map(Change::orderId).toList(), name);
            assertEquals(texts(c.get("skipped")), plan.skipped().stream().map(Skip::orderId).toList(), name);
            assertEquals(c.get("planHash").asString(), plan.planHash(), name);
        }
    }

    private static List<String> texts(JsonNode array) {
        List<String> list = new ArrayList<>();
        array.forEach(n -> list.add(n.asString()));
        return list;
    }
}
