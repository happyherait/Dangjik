package io.github.happyherait.dangjik.runbook;

/** 가상 PG사 원장에 남은 거래. 우리 DB와 별개로 PG가 알고 있는 사실이다. */
public record PgTransaction(String pgTxId, String orderId, String status, long amount) {
}
