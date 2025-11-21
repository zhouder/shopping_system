package com.cst.shopping_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderTime = new Date();

    @Column(length = 20, nullable = false)
    private String status = "CREATED";

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String address;
}
