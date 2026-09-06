package com.ecommerce.VougeVault.order.entity;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;   // FK kept for traceability/analytics only

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;   // snapshot — never re-read from ProductVariant later

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Column(name = "size_snapshot", nullable = false)
    private String sizeSnapshot;

    @Column(name = "color_snapshot", nullable = false)
    private String colorSnapshot;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}