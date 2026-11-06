package io.github.happyherait.dangjik.crm;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm")
public class CrmController {

    private final CrmRepository crm;

    public CrmController(CrmRepository crm) {
        this.crm = crm;
    }

    @GetMapping("/members")
    public List<CrmRepository.Member> members() {
        return crm.members();
    }

    @GetMapping("/members/{memberId}")
    public ResponseEntity<CrmRepository.Member360> member(@PathVariable String memberId) {
        return ResponseEntity.of(crm.member360(memberId));
    }

    @GetMapping("/inquiries")
    public List<CrmRepository.Inquiry> inquiries() {
        return crm.inquiries();
    }

    @GetMapping("/coupons")
    public List<CrmRepository.Coupon> coupons() {
        return crm.coupons();
    }

    @GetMapping("/segments")
    public List<CrmRepository.Segment> segments() {
        return crm.segments();
    }
}
