package io.github.happyherait.dangjik.partner;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 파트너 포털 API. 프로토타입이라 로그인 대신 화면에서 입점사와 사용자를 고른다.
 * 실제 서비스라면 입점사 번호는 로그인 정보에서 꺼내야 한다.
 */
@RestController
@RequestMapping("/api/partner")
public class PartnerController {

    private final PartnerRepository partners;
    private final ShipmentService shipments;

    public PartnerController(PartnerRepository partners, ShipmentService shipments) {
        this.partners = partners;
        this.shipments = shipments;
    }

    @GetMapping
    public List<PartnerRepository.Partner> list() {
        return partners.partners();
    }

    @GetMapping("/{partnerId}/dashboard")
    public ResponseEntity<PartnerRepository.Dashboard> dashboard(@PathVariable String partnerId) {
        return partners.partner(partnerId)
                .map(p -> ResponseEntity.ok(partners.dashboard(partnerId)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{partnerId}/products")
    public List<PartnerRepository.PartnerProduct> products(@PathVariable String partnerId) {
        return partners.products(partnerId);
    }

    @GetMapping("/{partnerId}/orders")
    public List<PartnerRepository.PartnerOrderLine> orders(@PathVariable String partnerId) {
        return partners.orderLines(partnerId);
    }

    @PostMapping("/{partnerId}/orders/{orderId}/lines/{lineNo}/ship")
    public ResponseEntity<Void> ship(@PathVariable String partnerId, @PathVariable String orderId,
                                     @PathVariable int lineNo,
                                     @RequestHeader(name = "X-Dangjik-User", defaultValue = "partner") String user) {
        shipments.ship(partnerId, orderId, lineNo, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{partnerId}/claims")
    public List<PartnerRepository.PartnerClaim> claims(@PathVariable String partnerId) {
        return partners.claims(partnerId);
    }

    @GetMapping("/{partnerId}/settlements")
    public List<PartnerRepository.Settlement> settlements(@PathVariable String partnerId) {
        return partners.settlements(partnerId);
    }
}
