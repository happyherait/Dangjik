package io.github.happyherait.dangjik.runbook;

/** 우리 DB에 있는 주문의 현재 상태. */
public record OrderSnapshot(String orderId, String status, long amount) {
}
