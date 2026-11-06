package io.github.happyherait.dangjik.mall;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MallController {

    private final ProductRepository products;
    private final OrderRepository orders;

    public MallController(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @GetMapping("/products")
    public List<ProductRepository.Product> products() {
        return products.findAll();
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderRepository.Order> order(@PathVariable String orderId) {
        return ResponseEntity.of(orders.findById(orderId));
    }
}
