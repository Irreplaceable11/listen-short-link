package io.listen.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_statistics")
public class ClickStatistic extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  //短码
  public String shortCode;

  //IPv4/IPv6地址
  public String ipAddress;

  //用户代理
  public String userAgent;

  //来源页面
  public String referer;

  //国家
  public String country;

  //城市
  public String city;

  //设备类型
  public String deviceType;

  //浏览器
  public String browser;

  //操作系统
  public String os;

  //点击时间
  public LocalDateTime clickTime;



}
