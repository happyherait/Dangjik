package io.github.happyherait.dangjik.mobile;

import java.util.regex.Pattern;

/** 앱 버전 비교와 업데이트 판단. DB나 Spring에 의존하지 않는다. */
public final class VersionPolicy {

    public record Decision(boolean forceUpdate, boolean updateAvailable, String latestVersion,
                           String minSupportedVersion, String message) {
    }

    private static final Pattern VERSION = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    private VersionPolicy() {
    }

    public static boolean isValid(String version) {
        return version != null && VERSION.matcher(version).matches();
    }

    /** a가 b보다 낮으면 음수, 같으면 0, 높으면 양수 */
    public static int compare(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int c = Integer.compare(Integer.parseInt(x[i]), Integer.parseInt(y[i]));
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    public static Decision decide(String current, String latest, String minSupported) {
        if (!isValid(current)) {
            return new Decision(true, true, latest, minSupported, "앱 버전을 알 수 없습니다. 최신 버전으로 업데이트해 주세요.");
        }
        if (compare(current, minSupported) < 0) {
            return new Decision(true, true, latest, minSupported,
                    "더 이상 지원하지 않는 버전입니다. " + latest + " 버전으로 업데이트해야 이용할 수 있습니다.");
        }
        if (compare(current, latest) < 0) {
            return new Decision(false, true, latest, minSupported, "새 버전 " + latest + "이 있습니다.");
        }
        return new Decision(false, false, latest, minSupported, "최신 버전입니다.");
    }
}
