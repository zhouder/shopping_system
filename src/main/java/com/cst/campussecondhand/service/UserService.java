package com.cst.campussecondhand.service;

import com.cst.campussecondhand.entity.User;
import com.cst.campussecondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.UUID;
import java.util.Objects;
import static com.cst.campussecondhand.service.ProductService.UPLOAD_DIR;
import org.springframework.transaction.annotation.Transactional;

@Service

public class UserService {
    @Autowired
    private UserRepository userRepository;

    // 注册逻辑（最小改动：支持前端选择“会员/商家”角色，并按角色生成编号）
    public User register(User user) {
        // 0) 用户名/邮箱唯一性
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("用户名 “" + user.getUsername() + "”已存在");
        }
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("该邮箱已注册");
        }

        // 1) 角色：只允许 MEMBER / SHOP_OWNER，其他一律置为 MEMBER
        String role = user.getRole();
        if (!"MEMBER".equalsIgnoreCase(role) && !"SHOP_OWNER".equalsIgnoreCase(role)) {
            role = "MEMBER";
        } else {
            role = role.toUpperCase(); // 统一成大写，前后端判断更稳定
        }
        user.setRole(role);

        // 2) 生成头像底色
        String[] colors = {"#f44336", "#e91e63", "#9c27b0", "#673ab7", "#3f51b5", "#2196f3", "#03a9f4", "#00bcd4", "#009688", "#4caf50", "#8bc34a", "#cddc39", "#ffeb3b", "#ffc107", "#ff9800", "#ff5722"};
        String bgColor = colors[(int) Math.floor(Math.random() * colors.length)];
        user.setAvatarBgColor(bgColor);

        // 3) 编号：按身份加前缀（S=商家 / M=会员）
        if (user.getMemberNo() == null || user.getMemberNo().isEmpty()) {
            String prefix = "SHOP_OWNER".equals(role) ? "S" : "M";
            // 时间+随机数，避免重复（和业务弱耦合）
            String ts = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
            String rnd = String.format("%04d", new java.util.Random().nextInt(10000));
            user.setMemberNo(prefix + ts + rnd);
        }

        // 4) 会员姓名：如果没传，用昵称兜底
        if (user.getRealName() == null || user.getRealName().isEmpty()) {
            user.setRealName(user.getNickname());
        }

        // 5) 地址：会员可先置空等待后续完善；商家不维护收货地址，置空即可
        if ("SHOP_OWNER".equals(role)) {
            user.setAddress(""); // 商家地址不使用
        } else {
            if (user.getAddress() == null) user.setAddress("");
        }

        // 6) 时间字段
        java.util.Date now = new java.util.Date();
        user.setCreatedTime(now);
        user.setUpdatedTime(now);

        // 7) 保存
        return userRepository.save(user);
    }


    // 登录逻辑
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 验证密码
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }

        // --- 新增逻辑 ---
        // 检查老用户是否有头像背景色，如果没有则为他生成一个
        if (user.getAvatarBgColor() == null || user.getAvatarBgColor().isEmpty()) {
            String[] colors = {"#f44336", "#e91e63", "#9c27b0", "#673ab7", "#3f51b5", "#2196f3", "#03a9f4", "#00bcd4", "#009688", "#4caf50", "#8bc34a", "#cddc39", "#ffeb3b", "#ffc107", "#ff9800", "#ff5722"};
            String bgColor = colors[(int) Math.floor(Math.random() * colors.length)];
            user.setAvatarBgColor(bgColor);
            userRepository.save(user); // 保存到数据库
        }
        // --- 新增逻辑结束 ---

        return user;
    }

    // @Transactional  // 可选，建议加
    public User updateUser(Integer userId, User userDetails) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 1) 邮箱唯一性校验：只有变更时才校验
        String newEmail = userDetails.getEmail();
        if (newEmail != null && !Objects.equals(newEmail, user.getEmail())) {
            User userByEmail = userRepository.findByEmail(newEmail);
            // 关键修复：用 Objects.equals 比较，避免在 int 上调用 equals
            if (userByEmail != null && !Objects.equals(userByEmail.getId(), userId)) {
                throw new RuntimeException("该邮箱已被注册");
            }
            user.setEmail(newEmail);
        }

        // 2) 其他字段：只在传入不为 null 时更新，避免把库里值清空
        if (userDetails.getNickname() != null) user.setNickname(userDetails.getNickname());
        if (userDetails.getPhone() != null)    user.setPhone(userDetails.getPhone());

        // 3) ★ 地址：按你的规则（商家不维护收货地址）写回
        if (!"SHOP_OWNER".equals(user.getRole()) && userDetails.getAddress() != null) {
            user.setAddress(userDetails.getAddress().trim());
        }

        user.setUpdatedTime(new Date());
        // 立刻持久化（save 也可以；saveAndFlush 便于你在日志里看到 UPDATE）
        return userRepository.saveAndFlush(user);
    }

    // 修改密码
    public void updatePassword(Integer userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 如果新密码为空或未改变，则不执行任何操作
        if (newPassword == null || newPassword.isEmpty() || user.getPassword().equals(newPassword)) {
            return;
        }

        user.setPassword(newPassword);
        user.setUpdatedTime(new Date());
        userRepository.save(user);
    }

    // 新增：更新用户头像
    public User updateAvatar(Integer userId, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 处理文件上传
        String uniqueFileName = UUID.randomUUID().toString() + "_" + avatarFile.getOriginalFilename();
        try {
            // 确保上传目录存在
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            Files.copy(avatarFile.getInputStream(), Paths.get(UPLOAD_DIR + uniqueFileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("头像上传失败", e);
        }

        String avatarUrl = "/uploads/" + uniqueFileName;
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedTime(new Date());

        return userRepository.save(user);
    }


    public User findById(Integer id) {
        return userRepository.findById(id).orElse(null);
    }

}
