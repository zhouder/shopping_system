package com.cst.campussecondhand.repository;

import com.cst.campussecondhand.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser_Id(Integer userId);
}
