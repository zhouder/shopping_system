package com.cst.campussecondhand.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name="user")
@Data

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //id可以自增
    private int id;

    @Column(nullable = false, unique = true, length=50)
    private String username; //用户名

    @Column(nullable = false)
    private String password; //密码

    @Column(length = 50)
    private String nickname; //昵称

    @Column(unique = true, length = 100)
    private String email; //邮箱

    @Column(length=20)
    private String phone; //电话

    private String avatarUrl; //头像的url
    private String avatarBgColor; // 用于生成默认头像的背景色

    @Column(name = "created_time")
    private Date createdTime; // 账号创建时间

    @Column(name = "updated_time")
    private Date updatedTime; // 最后更新时间

    @Column(name = "member_no", length = 20, unique = true)
    private String memberNo;    // 会员编号

    @Column(name = "real_name", length = 50)
    private String realName;    // 会员姓名（用于收货）

    @Column(name="address")
    private String address;     // 会员地址（邮寄地址）

    @Column(length = 20)
    private String role = "MEMBER"; // 角色：MEMBER（会员）、SHOP_OWNER（店主）、ADMIN（管理员）
}
