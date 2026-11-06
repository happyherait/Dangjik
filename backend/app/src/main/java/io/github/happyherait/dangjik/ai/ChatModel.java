package io.github.happyherait.dangjik.ai;

import java.util.List;
import java.util.Map;

/**
 * 대화 모델 한 번 호출. 메시지와 도구 형식은 Anthropic Messages API를 따른다.
 * 실제 Claude와 데모 모델, 테스트용 가짜 모델이 같은 인터페이스를 쓴다.
 */
public interface ChatModel {

    record Reply(List<Map<String, Object>> content, String stopReason) {
    }

    Reply next(String system, List<Map<String, Object>> messages, List<Map<String, Object>> tools);

    /** 화면에 보여줄 이름: claude 또는 demo */
    String mode();
}
