package io.github.happyherait.dangjik.runbook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** 계획 내용을 정해진 순서의 문자열로 만든 뒤 SHA-256으로 요약한다. */
public final class PlanHasher {

    private PlanHasher() {
    }

    public static String hash(String runbookId, List<Change> changes) {
        StringBuilder canonical = new StringBuilder(runbookId).append('\n');
        for (Change c : changes) {
            canonical.append(c.orderId()).append('|')
                    .append(c.fromStatus()).append('|')
                    .append(c.toStatus()).append('|')
                    .append(c.pgTxId()).append('|')
                    .append(c.amount()).append('\n');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
