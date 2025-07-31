package io.listen.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

  // 用户名
  public String username;

  // 邮箱
  public String email;

  // 密码哈希
  public String passwordHash;

  // API密钥
  public String apiKey;

  // 每日配额，默认1000
  public Integer quotaDaily;

  // 今日已用配额，默认0
  public Integer quotaUsedToday;

  // 配额重置日期
  public LocalDate quotaResetDate;

  // 状态：1-正常，0-禁用，默认1
  public Integer status;

  // 创建时间，默认为当前时间戳
  public LocalDateTime createdTime;

  // 更新时间，默认为当前时间戳，并在更新时自动更新
  public LocalDateTime updatedTime;
}
