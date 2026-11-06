package io.github.happyherait.dangjik.common;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 서버가 기록하는 시각. DB 서버의 시간대(Docker는 보통 UTC)에 기대지 않고
 * 업무 시간대(기본 Asia/Seoul)로 정해서 넘긴다. 시드 데이터도 같은 시간대로 적혀 있다.
 */
@Component
public class AppClock {

    private final Clock clock;

    public AppClock(@Value("${dangjik.zone:Asia/Seoul}") String zone) {
        this.clock = Clock.system(ZoneId.of(zone));
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
