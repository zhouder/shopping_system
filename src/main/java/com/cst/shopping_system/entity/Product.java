package com.cst.shopping_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "product")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String title; // 商品标题

    @Column(length = 1000)
    private String description; // 商品描述

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // 商品价格

    @Column(length = 2000)
    private String imageUrls; // 商品图片url

    @Column(name = "created_time")
    private Date createdTime; // 商品发布时间

    // 和user建立关系
    @ManyToOne(fetch = FetchType.EAGER) // 多对一
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller; // 卖家信息

    @Column(length = 50)
    private String category;  // 分类编号，对应 category 表的 code

    @Column(name = "sales", nullable = false)
    private int sales = 0; // 商品销量

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "favorite_count")
    private Integer favoriteCount = 0; // 收藏数，默认0
}
