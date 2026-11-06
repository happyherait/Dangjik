package io.github.happyherait.dangjik.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ChatModelConfig {

    /** ANTHROPIC_API_KEY가 있으면 실제 Claude, 없으면 데모 모델 */
    @Bean
    public ChatModel chatModel(RestClient.Builder builder, ObjectMapper json,
                               @Value("${dangjik.anthropic.api-key:}") String apiKey,
                               @Value("${dangjik.anthropic.model}") String model,
                               @Value("${dangjik.anthropic.base-url}") String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            return new DemoChatModel(json);
        }
        return new ClaudeChatModel(builder, baseUrl, apiKey, model);
    }
}
