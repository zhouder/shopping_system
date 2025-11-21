package com.cst.shopping_system.controller;

import com.cst.shopping_system.entity.Order;
import com.cst.shopping_system.entity.User;
import com.cst.shopping_system.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private User requireLogin(HttpSession session) {
        User u = (User) session.getAttribute("loggedInUser");
        if (u == null) throw new RuntimeException("UNAUTHORIZED");
        return u;
    }

    /** 从购物车结算生成订单 */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestParam(value = "address", required = false) String address,
                                      HttpSession session) {
        try {
            User u = requireLogin(session);
            Long orderId = orderService.checkout(u.getId(), address);
            Map<String, Object> body = new HashMap<>();
            body.put("orderId", orderId);
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /** 我的订单列表（不含明细） */
    @GetMapping("/my")
    public ResponseEntity<?> myOrders(HttpSession session) {
        try {
            User u = requireLogin(session);
            List<Order> list = orderService.listMyOrders(u.getId());

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Order o : list) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", o.getId());
                m.put("orderTime", o.getOrderTime());
                m.put("status", o.getStatus());
                m.put("totalAmount", o.getTotalAmount());
                m.put("address", o.getAddress());
                rows.add(m);
            }
            return ResponseEntity.ok(rows);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("请先登录");
        }
    }

    /** 订单详情（含头信息，明细可前端另查） */
    @GetMapping("/{id}")
    public ResponseEntity<?> orderDetail(@PathVariable("id") Long id, HttpSession session) {
        try {
            User u = requireLogin(session);
            Order o = orderService.getOrder(id, u.getId());

            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderTime", o.getOrderTime());
            m.put("status", o.getStatus());
            m.put("totalAmount", o.getTotalAmount());
            m.put("address", o.getAddress());
            return ResponseEntity.ok(m);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
