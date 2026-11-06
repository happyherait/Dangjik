package io.github.happyherait.dangjik.runbook;

import java.util.List;

/**
 * 런북 실행 계획(dry-run 결과). 승인은 planHash에 묶인다.
 * 승인 뒤 데이터가 바뀌어 계획이 달라지면 해시가 달라져 승인이 무효가 된다.
 */
public record RunbookPlan(String runbookId, List<Change> changes, List<Skip> skipped, String planHash) {

    public RunbookPlan {
        changes = List.copyOf(changes);
        skipped = List.copyOf(skipped);
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }
}
