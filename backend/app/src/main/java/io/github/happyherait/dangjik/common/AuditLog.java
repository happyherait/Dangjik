package io.github.happyherait.dangjik.common;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** 상태를 바꾸는 모든 작업은 감사 로그를 남긴다. */
@Component
public class AuditLog {

    private final JdbcClient jdbc;

    public AuditLog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String actorId, String action, String target, String detail) {
        jdbc.sql("""
                INSERT INTO audit_events (actor_id, action, target, detail, created_at)
                VALUES (:actor, :action, :target, :detail, SYSTIMESTAMP)
                """)
                .param("actor", actorId)
                .param("action", action)
                .param("target", target)
                .param("detail", detail)
                .update();
    }
}
