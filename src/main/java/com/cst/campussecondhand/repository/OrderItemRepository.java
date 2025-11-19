package com.cst.campussecondhand.repository;

import com.cst.campussecondhand.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
