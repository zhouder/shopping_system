package com.cst.shopping_system.controller;

import com.cst.shopping_system.entity.Product;
import com.cst.shopping_system.entity.User;
import com.cst.shopping_system.service.FavoriteService;
import com.cst.shopping_system.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private ProductService productService;

    @PostMapping("/{productId}")
    public ResponseEntity<?> toggleFavorite(@PathVariable Integer productId, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "请先登录"));
        }

        try {
            boolean isFavorited = favoriteService.toggleFavorite(loggedInUser.getId(), productId);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("isFavorited", isFavorited);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // 新增：获取当前用户的所有收藏商品
    @GetMapping("/my")
    public ResponseEntity<?> getMyFavorites(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "请先登录"));
        }

        try {
            List<Product> favoriteProducts = favoriteService.findFavoritesByUser(loggedInUser.getId());

            // 将商品列表转换为和首页/api/products接口一致的格式，方便前端复用渲染逻辑
            List<Map<String, Object>> productsResponse = favoriteProducts.stream().map(product -> {
                Map<String, Object> productMap = new java.util.HashMap<>();
                productMap.put("id", product.getId());
                productMap.put("title", product.getTitle());
                productMap.put("price", product.getPrice());
                productMap.put("sales", product.getSales());
                productMap.put("favoriteCount", product.getFavoriteCount());
                // 在“我的收藏”页面，所有商品当然都是已收藏状态
                productMap.put("isFavorited", true);

                if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                    productMap.put("coverImage", product.getImageUrls().split(",")[0]);
                } else {
                    productMap.put("coverImage", null);
                }

                Map<String, Object> sellerInfo = new java.util.HashMap<>();
                sellerInfo.put("id", product.getSeller().getId());
                sellerInfo.put("nickname", product.getSeller().getNickname());
                productMap.put("seller", sellerInfo);

                return productMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(productsResponse);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}