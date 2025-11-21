package com.cst.shopping_system.repository;

import com.cst.shopping_system.entity.Favorite;
import com.cst.shopping_system.entity.Product;
import com.cst.shopping_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    // 根据用户和商品查找收藏记录
    Optional<Favorite> findByUserAndProduct(User user, Product product);

    // 计算一个商品被多少用户收藏
    long countByProduct(Product product);

    // 检查用户是否收藏了某个商品
    boolean existsByUserAndProduct(User user, Product product);

    // 新增：根据用户查找其所有的收藏记录
    List<Favorite> findByUser(User user);
}