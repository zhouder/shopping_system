package com.cst.shopping_system.repository;

import com.cst.shopping_system.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    List<Product> findBySellerIdOrderByCreatedTimeDesc(Integer sellerId);
}
