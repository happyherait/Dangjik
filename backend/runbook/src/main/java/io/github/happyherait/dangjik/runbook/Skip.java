package io.github.happyherait.dangjik.runbook;

/** 요청됐지만 런북 대상에서 빠진 주문과 그 이유. */
public record Skip(String orderId, String reason) {
}
