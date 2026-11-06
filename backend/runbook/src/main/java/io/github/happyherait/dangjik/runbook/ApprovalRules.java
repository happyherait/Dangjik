package io.github.happyherait.dangjik.runbook;

/** 런북 실행 승인 규칙. AI 답변과 상관없이 이 규칙을 통과해야만 실행된다. */
public final class ApprovalRules {

    public static final String OPS_LEAD = "OPS_LEAD";

    private ApprovalRules() {
    }

    /**
     * @param requestedBy  런북 실행을 요청한 직원
     * @param approverId   승인하려는 직원
     * @param approverRole 승인하려는 직원의 역할
     * @param approvedHash 승인 화면에서 본 계획의 해시
     * @param current      실행 직전에 다시 계산한 계획
     */
    public static ApprovalDecision decide(String requestedBy, String approverId, String approverRole,
                                          String approvedHash, RunbookPlan current) {
        if (current.isEmpty()) {
            return ApprovalDecision.deny("실행할 변경이 없음");
        }
        if (requestedBy.equals(approverId)) {
            return ApprovalDecision.deny("요청자 본인은 승인할 수 없음");
        }
        if (!OPS_LEAD.equals(approverRole)) {
            return ApprovalDecision.deny("운영 팀장(OPS_LEAD)만 승인할 수 있음");
        }
        if (!current.planHash().equals(approvedHash)) {
            return ApprovalDecision.deny("승인 후 계획이 바뀌어 다시 승인해야 함");
        }
        return ApprovalDecision.allow();
    }
}
