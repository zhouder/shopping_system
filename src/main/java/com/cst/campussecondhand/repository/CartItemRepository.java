package com.cst.campussecondhand.repository;

import com.cst.campussecondhand.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 用 “_” 穿透到关联实体的 id 字段；类型使用 Integer，和 User/Product 的 id 对齐
    List<CartItem> findByUser_Id(Integer userId);

    Optional<CartItem> findByUser_IdAndProduct_Id(Integer userId, Integer productId);
}
