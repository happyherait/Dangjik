package io.github.happyherait.dangjik.ops;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** 배포, 웹훅 실패, 배치 경고, 고객 문의, 운영 조치를 한 줄의 시간순으로 모은다. */
@Service
public class TimelineService {

    public record Event(LocalDateTime at, String kind, String level, String title, String detail) {
    }

    private final OpsFactsRepository ops;

    public TimelineService(OpsFactsRepository ops) {
        this.ops = ops;
    }

    public List<Event> events() {
        List<Event> events = new ArrayList<>();
        ops.deployments().forEach(d -> events.add(new Event(d.deployedAt(), "DEPLOY", "INFO",
                d.area() + " " + d.version() + " 배포", d.note())));
        ops.failedWebhooks().forEach(w -> events.add(new Event(w.receivedAt(), "WEBHOOK", "ERROR",
                "PG 웹훅 실패 " + w.httpStatus() + " (" + w.orderId() + ")", w.message())));
        ops.batchJobs().stream().filter(b -> !"SUCCESS".equals(b.lastStatus()) && b.lastRunAt() != null)
                .forEach(b -> events.add(new Event(b.lastRunAt(), "BATCH", "WARN",
                        b.name() + " " + b.lastStatus(), b.lastMessage())));
        ops.auditEvents().forEach(a -> events.add(new Event(a.createdAt(), "AUDIT", "INFO",
                a.action() + " " + a.target(), a.detail())));
        events.sort(Comparator.comparing(Event::at).reversed());
        return events;
    }
}
