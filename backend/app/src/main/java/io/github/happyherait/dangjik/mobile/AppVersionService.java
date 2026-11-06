package io.github.happyherait.dangjik.mobile;

import io.github.happyherait.dangjik.common.ApiError;
import io.github.happyherait.dangjik.common.AppClock;
import io.github.happyherait.dangjik.common.AuditLog;
import io.github.happyherait.dangjik.ops.OpsFactsRepository;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppVersionService {

    private final JdbcClient jdbc;
    private final OpsFactsRepository facts;
    private final AuditLog audit;
    private final AppClock clock;

    public AppVersionService(JdbcClient jdbc, OpsFactsRepository facts, AuditLog audit, AppClock clock) {
        this.jdbc = jdbc;
        this.facts = facts;
        this.audit = audit;
        this.clock = clock;
    }

    public VersionPolicy.Decision check(String platform, String current) {
        OpsFactsRepository.AppVersion v = find(platform)
                .orElseThrow(() -> new ApiError.Conflict("알 수 없는 플랫폼입니다: " + platform));
        return VersionPolicy.decide(current, v.latestVersion(), v.minSupportedVersion());
    }

    /** 강제 업데이트 기준 변경. 운영 팀장만 할 수 있고 감사 로그를 남긴다. */
    @Transactional
    public OpsFactsRepository.AppVersion update(String platform, String latest, String minSupported, String userId) {
        if (!"OPS_LEAD".equals(facts.staffRole(userId).orElse(""))) {
            throw new ApiError.Conflict("운영 팀장(OPS_LEAD)만 앱 버전 기준을 바꿀 수 있습니다.");
        }
        if (!VersionPolicy.isValid(latest) || !VersionPolicy.isValid(minSupported)) {
            throw new ApiError.Conflict("버전은 3.8.0처럼 숫자 세 개로 입력하세요.");
        }
        if (VersionPolicy.compare(minSupported, latest) > 0) {
            throw new ApiError.Conflict("최소 지원 버전은 최신 버전보다 높을 수 없습니다.");
        }
        OpsFactsRepository.AppVersion before = find(platform)
                .orElseThrow(() -> new ApiError.Conflict("알 수 없는 플랫폼입니다: " + platform));
        jdbc.sql("""
                UPDATE app_versions SET latest_version = :latest, min_supported_version = :min, updated_at = :now
                 WHERE platform = :p
                """).param("latest", latest).param("min", minSupported).param("now", clock.now())
                .param("p", platform).update();
        audit.record(userId, "APP_VERSION_UPDATED", platform,
                "최신 " + before.latestVersion() + " → " + latest + ", 최소 " + before.minSupportedVersion() + " → " + minSupported);
        return find(platform).orElseThrow();
    }

    private Optional<OpsFactsRepository.AppVersion> find(String platform) {
        return facts.appVersions().stream().filter(v -> v.platform().equals(platform)).findFirst();
    }
}
