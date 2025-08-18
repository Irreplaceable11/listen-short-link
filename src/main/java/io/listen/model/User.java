package io.listen.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@UserDefinition
public class User extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  // 用户名
  @Username
  public String username;

  // 邮箱
  public String email;

  // 密码哈希
  @Password
  public String password;

  @Roles
  public String role;

  // API密钥
  public String apiKey;

  // 每日配额，默认1000
  //限制用户每天可以创建的短链数量
  public Integer quota;

  // 今日已用配额，默认0
  // 记录用户当天已经使用的配额数量
  public Integer quotaUsed;

  // 配额重置日期
  public LocalDate quotaResetDate;

  // 状态：1-正常，0-禁用，默认1
  public Integer status;

  // 创建时间，默认为当前时间戳
  public LocalDateTime createdTime;

  // 更新时间，默认为当前时间戳，并在更新时自动更新
  public LocalDateTime updatedTime;
}
