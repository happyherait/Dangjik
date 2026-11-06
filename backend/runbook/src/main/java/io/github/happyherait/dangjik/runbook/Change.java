package io.github.happyherait.dangjik.runbook;

/** 런북이 실행하면 일어날 변경 한 건. */
public record Change(String orderId, String fromStatus, String toStatus, String pgTxId, long amount) {
}
