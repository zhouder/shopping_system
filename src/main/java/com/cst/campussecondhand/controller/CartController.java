package com.cst.campussecondhand.controller;

import com.cst.campussecondhand.entity.CartItem;
import com.cst.campussecondhand.entity.User;
import com.cst.campussecondhand.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart-items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private User requireLogin(HttpSession session) {
        User u = (User) session.getAttribute("loggedInUser");
        if (u == null) throw new RuntimeException("UNAUTHORIZED");
        return u;
    }

    /** 加入购物车（默认数量=1） */
    @PostMapping
    public ResponseEntity<?> addItem(@RequestParam("productId") Integer productId,
                                     @RequestParam(value = "quantity", required = false, defaultValue = "1") Integer quantity,
                                     HttpSession session) {
        try {
            User u = requireLogin(session);
            CartItem item = cartService.addItem(u.getId(), productId, quantity);

            Map<String, Object> body = new HashMap<>();
            body.put("id", item.getId());                 // Long
            body.put("productId", item.getProduct().getId());
            body.put("quantity", item.getQuantity());     // Integer
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("请先登录");
        }
    }

    /** 我的购物车列表（附带商品信息、单价、小计、总价） */
    @GetMapping("/my")
    public ResponseEntity<?> myCart(HttpSession session) {
        try {
            User u = requireLogin(session);
            List<CartItem> list = cartService.listMyCart(u.getId());

            List<Map<String, Object>> rows = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;

            for (CartItem ci : list) {
                BigDecimal price = ci.getProduct().getPrice();
                BigDecimal sub = price.multiply(BigDecimal.valueOf(ci.getQuantity()));
                total = total.add(sub);

                Map<String, Object> m = new HashMap<>();
                m.put("id", ci.getId());                              // Long
                m.put("productId", ci.getProduct().getId());          // Long
                m.put("title", ci.getProduct().getTitle());           // String
                m.put("price", price);                                // BigDecimal
                m.put("quantity", ci.getQuantity());                  // Integer
                m.put("subtotal", sub);                               // BigDecimal
                rows.add(m);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("items", rows);
            body.put("totalAmount", total);
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("请先登录");
        }
    }

    /** 修改数量 */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id,
                                    @RequestParam("quantity") Integer quantity,
                                    HttpSession session) {
        try {
            User u = requireLogin(session);
            CartItem item = cartService.updateQuantity(u.getId(), id, quantity);

            Map<String, Object> body = new HashMap<>();
            body.put("id", item.getId());
            body.put("quantity", item.getQuantity());
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("请先登录");
        }
    }

    /** 删除购物车项 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id, HttpSession session) {
        try {
            User u = requireLogin(session);
            cartService.removeItem(u.getId(), id);

            Map<String, Object> body = new HashMap<>();
            body.put("deleted", Boolean.TRUE);
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("请先登录");
        }
    }
}
