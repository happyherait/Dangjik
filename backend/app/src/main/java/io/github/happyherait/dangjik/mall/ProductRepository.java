package io.github.happyherait.dangjik.mall;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    public record Product(String productId, String partnerId, String name, String category,
                          long price, int stock, String description, String status) {
    }

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Product> findAll() {
        return jdbc.sql("""
                SELECT product_id, partner_id, name, category, price, stock, description, status
                  FROM products
                 ORDER BY product_id
                """)
                .query(Product.class)
                .list();
    }
}
