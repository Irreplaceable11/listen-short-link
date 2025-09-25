package io.listen.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_mappings")
public class UrlMapping extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  // 短码
  public String shortCode;

  // 原始URL
  public String originalUrl;

  // 用户ID，可为空表示匿名
  public Long userId;

  // URL标题
  public String title;

  // 描述
  public String description;

  // 短链域名
  public String domain;

  // 点击次数，默认0
  public Long clickCount;

  // 状态：1-正常，0-禁用，默认1
  public Integer status;

  // 过期时间，NULL表示永不过期
  public LocalDateTime expireTime;

  public Long creator;

  // 创建时间，默认为当前时间戳
  public LocalDateTime createdTime;

  // 更新时间，默认为当前时间戳，并在更新时自动更新
  public LocalDateTime updatedTime;

  @WithSession
  public static Uni<String> findOriginalUrlByShortCode(String shortCode) {
    return find("select originalUrl from UrlMapping where shortCode = ?1", shortCode).project(String.class).firstResult();
  }
}
