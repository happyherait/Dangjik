package io.github.happyherait.dangjik.crm;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CrmRepository {

    public record Member(String memberId, String name, String email, String grade, LocalDate joinedAt) {
    }

    public record MemberOrder(String orderId, String channel, String status, long totalAmount,
                              LocalDateTime orderedAt) {
    }

    public record Inquiry(String inquiryId, String memberId, String memberName, String orderId, String channel,
                          String title, String body, String status, LocalDateTime createdAt) {
    }

    public record Member360(Member member, List<MemberOrder> orders, List<Inquiry> inquiries, long totalSpent) {
    }

    public record Coupon(String couponId, String name, int discountRate, LocalDate validTo, String status) {
    }

    public record Segment(String grade, int members) {
    }

    private static final String INQUIRY_SELECT = """
            SELECT q.inquiry_id, q.member_id, m.name AS member_name, q.order_id, q.channel, q.title, q.body,
                   q.status, q.created_at
              FROM cs_inquiries q JOIN members m ON m.member_id = q.member_id
            """;

    private final JdbcClient jdbc;

    public CrmRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Member> members() {
        return jdbc.sql("SELECT member_id, name, email, grade, joined_at FROM members ORDER BY member_id")
                .query(Member.class).list();
    }

    public Optional<Member360> member360(String memberId) {
        return jdbc.sql("SELECT member_id, name, email, grade, joined_at FROM members WHERE member_id = :m")
                .param("m", memberId).query(Member.class).optional()
                .map(member -> {
                    List<MemberOrder> orders = jdbc.sql("""
                            SELECT order_id, channel, status, total_amount, ordered_at
                              FROM orders WHERE member_id = :m ORDER BY ordered_at DESC
                            """).param("m", memberId).query(MemberOrder.class).list();
                    List<Inquiry> inquiries = jdbc.sql(INQUIRY_SELECT + " WHERE q.member_id = :m ORDER BY q.created_at DESC")
                            .param("m", memberId).query(Inquiry.class).list();
                    long spent = orders.stream()
                            .filter(o -> !"PAYMENT_PENDING".equals(o.status()) && !"CANCELED".equals(o.status()))
                            .mapToLong(MemberOrder::totalAmount).sum();
                    return new Member360(member, orders, inquiries, spent);
                });
    }

    public List<Inquiry> inquiries() {
        return jdbc.sql(INQUIRY_SELECT + " ORDER BY q.created_at DESC").query(Inquiry.class).list();
    }

    public List<Coupon> coupons() {
        return jdbc.sql("SELECT coupon_id, name, discount_rate, valid_to, status FROM coupons ORDER BY coupon_id")
                .query(Coupon.class).list();
    }

    public List<Segment> segments() {
        return jdbc.sql("""
                SELECT grade, COUNT(*) AS members FROM members GROUP BY grade
                 ORDER BY CASE grade WHEN 'VIP' THEN 1 WHEN 'GOLD' THEN 2 WHEN 'SILVER' THEN 3 ELSE 4 END
                """).query(Segment.class).list();
    }
}
