package com.cst.campussecondhand.service;

import com.cst.campussecondhand.entity.CartItem;
import com.cst.campussecondhand.entity.Product;
import com.cst.campussecondhand.entity.User;
import com.cst.campussecondhand.repository.CartItemRepository;
import com.cst.campussecondhand.repository.ProductRepository;
import com.cst.campussecondhand.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    public List<CartItem> listMyCart(Integer userId) {
        return cartRepo.findByUser_Id(userId);
    }

    @Transactional
    public CartItem addItem(Integer userId, Integer productId, int quantity) {
        // 先规范数量，但后面不再改这个变量
        int q = quantity <= 0 ? 1 : quantity;

        // 查询是否已有同一商品的购物车项
        Optional<CartItem> opt = cartRepo.findByUser_IdAndProduct_Id(userId, productId);
        if (opt.isPresent()) {
            CartItem item = opt.get();
            item.setQuantity(item.getQuantity() + q);
            return cartRepo.save(item);
        } else {
            User u = userRepo.findById(userId).orElseThrow();
            Product p = productRepo.findById(productId).orElseThrow();
            CartItem item = new CartItem();
            item.setUser(u);
            item.setProduct(p);
            item.setQuantity(q);
            return cartRepo.save(item);
        }
    }


    @Transactional
    public CartItem updateQuantity(Integer userId, Long cartItemId, int quantity) {
        if (quantity < 1) quantity = 1;
        CartItem item = cartRepo.findById(cartItemId).orElseThrow();
        // 注意：让 Integer 去比较，避免对 int 调用 equals
        if (!userId.equals(item.getUser().getId())) {
            throw new RuntimeException("无权操作该购物车项");
        }
        item.setQuantity(quantity);
        return cartRepo.save(item);
    }

    @Transactional
    public void removeItem(Integer userId, Long cartItemId) {
        CartItem item = cartRepo.findById(cartItemId).orElseThrow();
        if (!userId.equals(item.getUser().getId())) {
            throw new RuntimeException("无权操作该购物车项");
        }
        cartRepo.delete(item);
    }
}
