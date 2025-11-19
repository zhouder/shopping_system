package com.cst.campussecondhand.service;

import com.cst.campussecondhand.entity.*;
import com.cst.campussecondhand.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    @Transactional
    public Long checkout(Integer userId, String addressFromClient) {
        // 1) 读取购物车
        List<CartItem> cartItems = cartRepo.findByUser_Id(userId);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("购物车为空，无法下单");
        }

        // 2) 创建订单
        User user = userRepo.findById(userId).orElseThrow();
        Order order = new Order();
        order.setUser(user);
        order.setOrderTime(new Date());
        order.setStatus("CREATED");
        order.setAddress((addressFromClient != null && !addressFromClient.isEmpty())
                ? addressFromClient : user.getAddress());
        order.setTotalAmount(BigDecimal.ZERO);
        order = orderRepo.save(order);

        // 3) 明细 + 总价 + 累加销量
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            Product p = ci.getProduct();
            int qty = ci.getQuantity();
            BigDecimal unitPrice = p.getPrice();

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(p);
            item.setQuantity(qty);
            item.setUnitPrice(unitPrice);
            orderItemRepo.save(item);

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(qty)));

            // 累加销量
            p.setSales(p.getSales() + qty);
            productRepo.save(p);
        }

        // 4) 写回总价并清空购物车
        order.setTotalAmount(total);
        orderRepo.save(order);
        cartRepo.deleteAll(cartItems);

        return order.getId();
    }

    public List<Order> listMyOrders(Integer userId) {
        return orderRepo.findByUser_Id(userId);
    }

    public Order getOrder(Long orderId, Integer userId) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        // 关键修复：让 Integer（userId）来比较，而不是对 int 调用 equals
        if (!userId.equals(o.getUser().getId())) {
            throw new RuntimeException("无权访问该订单");
        }
        return o;
    }
}
