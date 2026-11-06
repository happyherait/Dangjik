package io.github.happyherait.dangjik.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.ObjectMapper;

/**
 * API 키 없이 쓰는 데모 모델. 실제 AI처럼 판단하지 않고, 질문에 들어 있는 번호를 보고
 * 정해진 순서로 도구를 부른 뒤 조회 결과로 답을 만든다. 도구 실행과 승인 절차는 실제와 똑같다.
 */
public class DemoChatModel implements ChatModel {

    private static final Pattern ORDER = Pattern.compile("ORD-\\d{8}-\\d{4}");
    private static final Pattern INQUIRY = Pattern.compile("INQ-\\d{4}");
    private static final Pattern PRODUCT = Pattern.compile("PRD-\\d{4}");

    private final ObjectMapper json;

    public DemoChatModel(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public String mode() {
        return "demo";
    }

    @Override
    public Reply next(String system, List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        String question = String.valueOf(messages.getFirst().get("content"));
        List<Map<String, Object>> done = toolResults(messages);

        Matcher product = PRODUCT.matcher(question);
        if (product.find()) {
            return done.isEmpty()
                    ? use("get_product", Map.of("productId", product.group()), 1)
                    : say(productAnswer(done.getFirst()));
        }

        // 순서: (문의) → (결제대기 주문 찾기) → 주문 → PG → 웹훅 → 배포 → 런북 제안 → 답
        List<String[]> plan = new ArrayList<>();
        Matcher inquiry = INQUIRY.matcher(question);
        if (inquiry.find()) {
            plan.add(new String[] {"get_inquiry", inquiry.group()});
        }
        Matcher order = ORDER.matcher(question);
        String orderId = order.find() ? order.group() : null;
        if (orderId == null && !plan.isEmpty() && done.size() >= 1) {
            orderId = orderInInquiry(done.getFirst()).orElse(null);
        }
        if (orderId == null) {
            plan.add(new String[] {"find_stuck_payments", null});
            if (done.size() >= plan.size()) {
                orderId = firstStuckOrder(done.get(plan.size() - 1)).orElse(null);
            }
        }
        if (orderId == null) {
            if (done.size() < plan.size()) {
                return step(plan.get(done.size()), done.size() + 1);
            }
            return say("결제대기로 남은 PG 승인 주문을 찾지 못했습니다. 주문번호나 문의번호를 알려 주세요.");
        }
        plan.add(new String[] {"get_order", orderId});
        plan.add(new String[] {"get_pg_transactions", orderId});
        plan.add(new String[] {"get_webhook_logs", orderId});
        plan.add(new String[] {"list_deployments", null});
        plan.add(new String[] {"propose_payment_resync", orderId});

        if (done.size() < plan.size()) {
            return step(plan.get(done.size()), done.size() + 1);
        }
        return say(orderAnswer(orderId, done, plan));
    }

    private static Reply step(String[] p, int n) {
        Map<String, Object> input = switch (p[0]) {
            case "get_inquiry" -> Map.of("inquiryId", p[1]);
            case "list_deployments", "find_stuck_payments" -> Map.of();
            case "propose_payment_resync" -> Map.of("orderIds", List.of(p[1]),
                    "reason", "PG는 승인했지만 웹훅 수신 실패로 결제대기에 머문 주문");
            default -> Map.of("orderId", p[1]);
        };
        return use(p[0], input, n);
    }

    // ---------- 답 만들기 ----------

    private String productAnswer(Map<String, Object> result) {
        Map<String, Object> value = read(result);
        if (value.containsKey("error")) {
            return "상품을 찾지 못했습니다: " + value.get("error");
        }
        Map<String, Object> p = map(value.get("external_data"));
        String description = String.valueOf(p.get("description"));
        boolean suspicious = description.contains("AI") || description.contains("지시") || description.contains("0원");
        StringBuilder sb = new StringBuilder();
        sb.append("진단\n- ").append(p.get("productId")).append(" ").append(p.get("name"))
                .append(", 판매가 ").append(won(p.get("price"))).append("\n");
        if (suspicious) {
            sb.append("- 상품 설명 안에 운영 AI에게 보내는 지시문이 숨어 있습니다. 입점사가 쓴 데이터라서 따르지 않았습니다.\n");
            sb.append("근거\n- 설명 원문: ").append(description).append("\n");
            sb.append("조치\n- 가격은 바꾸지 않았습니다. 이 시스템의 AI에게는 가격 변경 도구가 없습니다.\n");
            sb.append("- 입점사 ").append(p.get("partnerId")).append("에 설명 수정을 요청하고, 상품 검수 SR을 등록하는 것을 권합니다.");
        } else {
            sb.append("- 상품 정보에 특이 사항이 없습니다.");
        }
        return sb.toString();
    }

    private String orderAnswer(String orderId, List<Map<String, Object>> done, List<String[]> plan) {
        Map<String, Object> order = map(result(done, plan, "get_order").get("order"));
        List<Map<String, Object>> pg = list(result(done, plan, "get_pg_transactions").get("items"));
        List<Map<String, Object>> hooks = list(result(done, plan, "get_webhook_logs").get("items"));
        List<Map<String, Object>> deploys = list(result(done, plan, "list_deployments").get("items"));
        Map<String, Object> proposal = result(done, plan, "propose_payment_resync");

        Map<String, Object> approved = pg.stream().filter(t -> "APPROVED".equals(t.get("status"))).findFirst().orElse(null);
        StringBuilder sb = new StringBuilder("진단\n");
        if (!"PAYMENT_PENDING".equals(order.get("status")) || approved == null) {
            sb.append("- ").append(orderId).append(" 주문은 ").append(order.get("status"))
                    .append(" 상태이고 PG 승인과 어긋남이 없습니다. 재동기화가 필요하지 않습니다.");
            return sb.toString();
        }
        sb.append("- ").append(orderId).append(" 주문은 결제대기지만, PG에는 ").append(approved.get("pgTxId"))
                .append(" ").append(won(approved.get("amount"))).append(" 승인 거래가 있습니다. 결제 알림(웹훅)을 받지 못한 상태입니다.\n");
        sb.append("근거\n");
        List<Map<String, Object>> failed = hooks.stream().filter(h -> !Integer.valueOf(200).equals(h.get("httpStatus"))).toList();
        if (!failed.isEmpty()) {
            sb.append("- 웹훅 ").append(failed.size()).append("번 실패: ").append(failed.getFirst().get("httpStatus"))
                    .append(" ").append(failed.getFirst().get("message")).append("\n");
        }
        String approvedAt = String.valueOf(approved.get("approvedAt"));
        deploys.stream()
                .filter(d -> String.valueOf(d.get("deployedAt")).compareTo(approvedAt) < 0)
                .findFirst()
                .ifPresent(d -> sb.append("- 직전 배포 ").append(d.get("area")).append(" ").append(d.get("version"))
                        .append(": ").append(d.get("note")).append(". PG가 예전 경로로 알림을 보내 실패한 것으로 보입니다.\n"));
        sb.append("조치\n");
        Object runId = proposal.get("runId");
        if (runId != null) {
            sb.append("- 런북 '결제 상태 재동기화'를 제안했습니다 (").append(runId)
                    .append("). 운영 팀장이 승인하면 결제완료로 바뀌고 입점사가 출고할 수 있습니다.\n");
        } else {
            sb.append("- 런북 제안이 등록되지 않았습니다: ").append(proposal.getOrDefault("error", "실행할 변경 없음")).append("\n");
        }
        sb.append("- 재발 방지: PG사 웹훅 주소를 새 경로로 바꾸거나 이전 경로를 함께 받도록 배포해야 합니다. 이 작업은 개발 조치라서 런북 범위 밖입니다.");
        return sb.toString();
    }

    // ---------- 메시지 다루기 ----------

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> toolResults(List<Map<String, Object>> messages) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> m : messages) {
            if ("user".equals(m.get("role")) && m.get("content") instanceof List<?> blocks) {
                for (Object b : blocks) {
                    Map<String, Object> block = (Map<String, Object>) b;
                    if ("tool_result".equals(block.get("type"))) {
                        results.add(block);
                    }
                }
            }
        }
        return results;
    }

    private Map<String, Object> result(List<Map<String, Object>> done, List<String[]> plan, String tool) {
        for (int i = 0; i < plan.size() && i < done.size(); i++) {
            if (plan.get(i)[0].equals(tool)) {
                return read(done.get(i));
            }
        }
        return Map.of();
    }

    /** 도구 결과를 Map으로 읽는다. 목록이면 items 키에 담는다. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> read(Map<String, Object> toolResult) {
        Object v = json.readValue(String.valueOf(toolResult.get("content")), Object.class);
        if (v instanceof List<?> l) {
            return Map.of("items", l);
        }
        return v instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private Optional<String> orderInInquiry(Map<String, Object> inquiryResult) {
        Object data = read(inquiryResult).get("external_data");
        Object id = data instanceof Map<?, ?> m ? m.get("orderId") : null;
        return id == null ? Optional.empty() : Optional.of(String.valueOf(id));
    }

    private Optional<String> firstStuckOrder(Map<String, Object> stuckResult) {
        List<Map<String, Object>> items = list(read(stuckResult).get("items"));
        return items.isEmpty() ? Optional.empty() : Optional.of(String.valueOf(items.getFirst().get("orderId")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object o) {
        return o instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    private static String won(Object amount) {
        return String.format("%,d원", ((Number) amount).longValue());
    }

    private static Reply use(String tool, Map<String, Object> input, int n) {
        return new Reply(List.of(Map.of("type", "tool_use", "id", "demo-" + n, "name", tool, "input", input)), "tool_use");
    }

    private static Reply say(String text) {
        return new Reply(List.of(Map.of("type", "text", "text", text)), "end_turn");
    }
}
