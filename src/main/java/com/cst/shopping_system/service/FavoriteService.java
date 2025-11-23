package com.cst.shopping_system.service;

import com.cst.shopping_system.entity.Favorite;
import com.cst.shopping_system.entity.Product;
import com.cst.shopping_system.entity.User;
import com.cst.shopping_system.repository.FavoriteRepository;
import com.cst.shopping_system.repository.ProductRepository;
import com.cst.shopping_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public boolean toggleFavorite(Integer userId, Integer productId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("商品不存在"));

        Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndProduct(user, product);
        boolean isNowFavorited;

        if (existingFavorite.isPresent()) {
            // 1. 取消收藏
            favoriteRepository.delete(existingFavorite.get());
            // 减少计数 (防止减成负数)
            int current = product.getFavoriteCount() == null ? 0 : product.getFavoriteCount();
            product.setFavoriteCount(Math.max(0, current - 1));
            isNowFavorited = false;
        } else {
            // 2. 添加收藏
            favoriteRepository.save(new Favorite(user, product));
            // 增加计数
            int current = product.getFavoriteCount() == null ? 0 : product.getFavoriteCount();
            product.setFavoriteCount(current + 1);
            isNowFavorited = true;
        }

        // 3. ★关键步骤★：保存商品最新的收藏数量到数据库
        productRepository.save(product);

        return isNowFavorited;
    }

    // 新增：获取一个用户收藏的所有商品
    public List<Product> findFavoritesByUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<Favorite> favorites = favoriteRepository.findByUser(user);

        // 从收藏列表中提取出所有的商品并返回
        return favorites.stream()
                .map(Favorite::getProduct)
                .collect(Collectors.toList());
    }
}