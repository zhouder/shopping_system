package com.cst.shopping_system.controller;

import com.cst.shopping_system.entity.Order;
import com.cst.shopping_system.entity.OrderItem;
import com.cst.shopping_system.repository.OrderItemRepository;
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
    private final OrderItemRepository orderItemRepo;

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

    /** 订单详情（含商品明细） */
    @GetMapping("/{id}")
    public ResponseEntity<?> orderDetail(@PathVariable("id") Long id, HttpSession session) {
        try {
            User u = requireLogin(session);
            // 1. 获取订单基本信息 (鉴权逻辑在 getOrder 内部)
            Order o = orderService.getOrder(id, u.getId());

            // 2. 获取订单包含的商品明细
            List<OrderItem> items = orderItemRepo.findByOrder_Id(o.getId());

            // 3. 组装返回数据
            Map<String, Object> result = new HashMap<>();

            // 3.1 订单头信息
            Map<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("id", o.getId());
            orderInfo.put("orderTime", o.getOrderTime());
            orderInfo.put("status", o.getStatus());
            orderInfo.put("totalAmount", o.getTotalAmount());
            orderInfo.put("address", o.getAddress());
            result.put("order", orderInfo);

            // 3.2 商品明细列表 (提取需要展示的字段)
            List<Map<String, Object>> itemStats = items.stream().map(item -> {
                Map<String, Object> m = new HashMap<>();
                m.put("productId", item.getProduct().getId());
                m.put("title", item.getProduct().getTitle());
                // 处理图片：取第一张，或者默认图
                String cover = null;
                if (item.getProduct().getImageUrls() != null && !item.getProduct().getImageUrls().isEmpty()) {
                    cover = item.getProduct().getImageUrls().split(",")[0];
                }
                m.put("coverImage", cover);
                m.put("price", item.getUnitPrice());
                m.put("quantity", item.getQuantity());
                return m;
            }).toList();

            result.put("items", itemStats);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
