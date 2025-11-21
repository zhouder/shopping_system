package com.cst.shopping_system.controller;

import com.cst.shopping_system.entity.User;
import com.cst.shopping_system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
// import 放到文件顶部其它 import 旁边
import com.cst.shopping_system.repository.UserRepository;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")

public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try{
            // 注册成功返回一个成功的响应，为了安全，清空密码再返回
            User registeredUser=userService.register(user);
            registeredUser.setPassword(null);
            return ResponseEntity.ok(registeredUser);
        }catch (RuntimeException e){
            Map<String,String> error= Collections.singletonMap("error",e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error); // 400
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials, HttpSession session) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            User user = userService.login(username, password);

            // 登录成功，将用户信息存入 session
            session.setAttribute("loggedInUser", user);

            // 登录成功，返回用户信息（同样，为了安全，清空密码）
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            Map<String, String> error = Collections.singletonMap("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);  // 401
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpSession session) {
        // 让 session 失效
        session.invalidate();
        return ResponseEntity.ok(Collections.singletonMap("message", "退出登录成功"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getUserStatus(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser != null) {
            // 用户已登录，返回用户信息
            loggedInUser.setPassword(null); // 确保不返回密码
            return ResponseEntity.ok(loggedInUser);
        }
        // 用户未登录
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未登录"));
    }

    // 获取当前登录用户的完整信息 (用于个人中心)
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未登录"));
        }
        // 从数据库重新获取最新的用户信息
        User latestUser = userService.findById(loggedInUser.getId());
        //latestUser.setPassword("******"); // 隐藏密码
        return ResponseEntity.ok(latestUser);
    }

    // 更新用户基本信息
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(@RequestBody User userDetails, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未登录"));
        }
        try {
            User updatedUser = userService.updateUser(loggedInUser.getId(), userDetails);
            updatedUser.setPassword(null); // 不返回密码
            // 更新 session 中的用户信息
            session.setAttribute("loggedInUser", updatedUser);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // 修改密码
    @PutMapping("/me/password")
    public ResponseEntity<?> updateMyPassword(@RequestBody Map<String, String> passwordMap, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未登录"));
        }
        try {
            String newPassword = passwordMap.get("newPassword");
            // 调用简化后的方法
            userService.updatePassword(loggedInUser.getId(), newPassword);
            return ResponseEntity.ok(Collections.singletonMap("message", "密码修改成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // 新增：处理头像上传
    @PostMapping("/me/avatar")
    public ResponseEntity<?> updateMyAvatar(@RequestParam("avatar") MultipartFile avatarFile, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "用户未登录"));
        }
        try {
            User updatedUser = userService.updateAvatar(loggedInUser.getId(), avatarFile);
            updatedUser.setPassword(null);
            session.setAttribute("loggedInUser", updatedUser);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // 新增：根据用户ID获取用户的公开信息（用于聊天等场景）
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserPublicProfile(@PathVariable Integer id) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            // 只返回公开信息，隐藏密码等敏感数据
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/api/users/me")
    public ResponseEntity<?> me(HttpSession session) {
        User su = (User) session.getAttribute("loggedInUser");
        if (su == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登录");
        }
        // ★ 从数据库读“最新”的用户，避免 session 里还是旧对象
        User u = userRepository.findById(su.getId()).orElse(su);
        session.setAttribute("loggedInUser", u); // 同步回 session

        Map<String, Object> body = new HashMap<>();
        body.put("id",        u.getId());
        body.put("username",  u.getUsername());
        body.put("nickname",  u.getNickname());
        body.put("role",      u.getRole());
        body.put("memberNo",  u.getMemberNo());
        // ★ address 只放一次；店主不返回
        body.put("address", "SHOP_OWNER".equals(u.getRole()) ? null : u.getAddress());
        body.put("email",     u.getEmail());
        body.put("phone",     u.getPhone());
        body.put("avatarUrl", u.getAvatarUrl());
        body.put("avatarBgColor", u.getAvatarBgColor());
        return ResponseEntity.ok(body);
    }

    @PutMapping(path = "/api/users/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateMe(@RequestBody Map<String, Object> req,
                                      HttpSession session) {
        User u = (User) session.getAttribute("loggedInUser");
        if (u == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未登录");
        }
        if (req.containsKey("address")) {
            u.setAddress((String) req.get("address")); // ① 设置地址
        }

        userRepository.saveAndFlush(u);                // ② 落库（save 也可以，saveAndFlush更直观）
        session.setAttribute("loggedInUser", u);       // ③ 刷新 session，避免 /me 读旧值

        if (req.containsKey("nickname")) u.setNickname((String) req.get("nickname"));
        if (req.containsKey("email"))    u.setEmail((String) req.get("email"));
        if (req.containsKey("phone"))    u.setPhone((String) req.get("phone"));
        if (req.containsKey("address"))  u.setAddress((String) req.get("address")); // ★ 关键

        userRepository.save(u);                    // ★ 落库
        session.setAttribute("loggedInUser", u);   // ★ 刷新 session，避免 /me 读到旧值

        Map<String,Object> body = new HashMap<>();
        body.put("nickname", u.getNickname());
        body.put("email",    u.getEmail());
        body.put("phone",    u.getPhone());
        body.put("address",  u.getAddress());
        return ResponseEntity.ok(body);
    }


}