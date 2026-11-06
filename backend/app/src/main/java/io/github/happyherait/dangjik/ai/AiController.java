package io.github.happyherait.dangjik.ai;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops/chat")
public class AiController {

    public record Question(String message, String user, String ticketId) {
    }

    private final DutyAgent agent;

    public AiController(DutyAgent agent) {
        this.agent = agent;
    }

    @GetMapping("/mode")
    public Map<String, String> mode() {
        return Map.of("mode", agent.mode());
    }

    @PostMapping
    public DutyAgent.Answer ask(@RequestBody Question q) {
        return agent.ask(q.message(), q.user(), q.ticketId());
    }
}
