package io.github.happyherait.dangjik.ai;

import io.github.happyherait.dangjik.common.ApiError;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Anthropic Messages API 호출. API 키는 환경 변수로만 받고 로그에 남기지 않는다. */
public class ClaudeChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(ClaudeChatModel.class);

    private final RestClient http;
    private final String apiKey;
    private final String model;

    public ClaudeChatModel(RestClient.Builder builder, String baseUrl, String apiKey, String model) {
        this.http = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Reply next(String system, List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 2000,
                "system", system,
                "messages", messages,
                "tools", tools);
        try {
            Map<String, Object> response = http.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return new Reply((List<Map<String, Object>>) response.get("content"), (String) response.get("stop_reason"));
        } catch (RestClientResponseException e) {
            log.warn("Anthropic API 오류 {} model={} body={}", e.getStatusCode().value(), model,
                    e.getResponseBodyAsString());
            throw new ApiError.Conflict(failure(e.getStatusCode().value()));
        } catch (RestClientException e) {
            log.warn("Anthropic API 연결 실패 model={}", model, e);
            throw new ApiError.Conflict("AI 서버에 연결하지 못했습니다. 네트워크를 확인하세요.");
        }
    }

    @Override
    public String mode() {
        return "claude";
    }

    private static String failure(int status) {
        String hint = switch (status) {
            case 400 -> "요청 형식이나 크레딧을 확인하세요.";
            case 401, 403 -> "API 키를 확인하세요.";
            case 404 -> "모델 이름을 확인하세요.";
            case 429 -> "호출 한도를 넘었습니다. 잠시 후 다시 시도하세요.";
            default -> "잠시 후 다시 시도하세요.";
        };
        return "AI 호출에 실패했습니다 (HTTP " + status + "). " + hint;
    }
}
