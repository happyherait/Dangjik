package io.github.happyherait.dangjik.runbook;

public record ApprovalDecision(boolean allowed, String reason) {

    static ApprovalDecision allow() {
        return new ApprovalDecision(true, "승인 가능");
    }

    static ApprovalDecision deny(String reason) {
        return new ApprovalDecision(false, reason);
    }
}
