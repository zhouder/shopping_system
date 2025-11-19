package com.cst.campussecondhand.service;

import com.cst.campussecondhand.entity.Favorite;
import com.cst.campussecondhand.entity.Product;
import com.cst.campussecondhand.entity.User;
import com.cst.campussecondhand.repository.FavoriteRepository;
import com.cst.campussecondhand.repository.ProductRepository;
import com.cst.campussecondhand.repository.UserRepository;
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

        if (existingFavorite.isPresent()) {
            // 如果已收藏，则取消收藏
            favoriteRepository.delete(existingFavorite.get());
            return false; // 返回 false 表示取消收藏
        } else {
            // 如果未收藏，则添加收藏
            favoriteRepository.save(new Favorite(user, product));
            return true; // 返回 true 表示添加收藏
        }

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