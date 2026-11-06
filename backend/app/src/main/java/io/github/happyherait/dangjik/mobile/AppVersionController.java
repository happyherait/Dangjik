package io.github.happyherait.dangjik.mobile;

import io.github.happyherait.dangjik.ops.OpsFactsRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppVersionController {

    public record UpdateRequest(String latestVersion, String minSupportedVersion, String userId) {
    }

    private final AppVersionService versions;

    public AppVersionController(AppVersionService versions) {
        this.versions = versions;
    }

    /** 앱이 켜질 때 부른다. 최소 지원 버전보다 낮으면 forceUpdate=true */
    @GetMapping("/api/app/version-check")
    public VersionPolicy.Decision check(@RequestParam String platform, @RequestParam String version) {
        return versions.check(platform, version);
    }

    @PutMapping("/api/ops/app-versions/{platform}")
    public OpsFactsRepository.AppVersion update(@PathVariable String platform, @RequestBody UpdateRequest r) {
        return versions.update(platform, r.latestVersion(), r.minSupportedVersion(), r.userId());
    }
}
