package io.github.happyherait.dangjik.ops;

import io.github.happyherait.dangjik.runbook.RunbookPlan;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영 콘솔 API. 프로토타입이라 로그인 대신 요청 본문의 직원 아이디를 쓴다.
 * 실제 서비스라면 직원 아이디는 로그인 정보에서 꺼내야 한다.
 */
@RestController
@RequestMapping("/api/ops")
public class OpsController {

    public record ProposeRequest(String runbookId, List<String> orderIds, String reason, String requestedBy,
                                 String ticketId) {
    }

    public record ApproveRequest(String approverId, String planHash) {
    }

    public record RejectRequest(String approverId, String reason) {
    }

    private final RunbookService runbooks;
    private final TicketRepository tickets;
    private final TimelineService timeline;
    private final OpsFactsRepository facts;

    public OpsController(RunbookService runbooks, TicketRepository tickets, TimelineService timeline,
                         OpsFactsRepository facts) {
        this.runbooks = runbooks;
        this.tickets = tickets;
        this.timeline = timeline;
        this.facts = facts;
    }

    @GetMapping("/runbook-runs")
    public List<RunbookService.RunView> runs() {
        return runbooks.runs();
    }

    @GetMapping("/runbook-runs/{runId}")
    public ResponseEntity<RunbookService.RunView> run(@PathVariable String runId) {
        return ResponseEntity.of(runbooks.run(runId));
    }

    /** 실행 전 미리보기. DB를 바꾸지 않는다. */
    @GetMapping("/runbooks/{runbookId}/plan")
    public RunbookPlan preview(@PathVariable String runbookId, @RequestParam List<String> orderIds) {
        return runbooks.plan(runbookId, orderIds);
    }

    @PostMapping("/runbook-runs")
    public RunbookService.Proposal propose(@RequestBody ProposeRequest r) {
        return runbooks.propose(r.runbookId(), r.orderIds(), r.reason(), r.requestedBy(), r.ticketId());
    }

    @PostMapping("/runbook-runs/{runId}/approve")
    public RunbookService.RunView approve(@PathVariable String runId, @RequestBody ApproveRequest r) {
        return runbooks.approve(runId, r.approverId(), r.planHash());
    }

    @PostMapping("/runbook-runs/{runId}/reject")
    public RunbookService.RunView reject(@PathVariable String runId, @RequestBody RejectRequest r) {
        return runbooks.reject(runId, r.approverId(), r.reason());
    }

    @GetMapping("/tickets")
    public List<TicketRepository.Ticket> tickets() {
        return tickets.list();
    }

    @PostMapping("/tickets")
    public TicketRepository.Ticket createTicket(@RequestBody TicketRepository.NewTicket t) {
        return tickets.create(t);
    }

    @GetMapping("/timeline")
    public List<TimelineService.Event> timeline() {
        return timeline.events();
    }

    @GetMapping("/stuck-payments")
    public List<OpsFactsRepository.StuckPayment> stuckPayments() {
        return facts.stuckPayments();
    }

    @GetMapping("/deployments")
    public List<OpsFactsRepository.Deployment> deployments() {
        return facts.deployments();
    }

    @GetMapping("/batches")
    public List<OpsFactsRepository.BatchJob> batches() {
        return facts.batchJobs();
    }

    @GetMapping("/app-versions")
    public List<OpsFactsRepository.AppVersion> appVersions() {
        return facts.appVersions();
    }

    @GetMapping("/audit")
    public List<OpsFactsRepository.AuditEvent> audit() {
        return facts.auditEvents();
    }
}
