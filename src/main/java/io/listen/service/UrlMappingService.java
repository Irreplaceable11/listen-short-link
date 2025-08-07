package io.listen.service;

import io.listen.dto.request.CreateShortUrlRequest;
import io.listen.enums.StatusEnum;
import io.listen.generator.ShortCodeGenerator;
import io.listen.model.ClickStatistic;
import io.listen.model.UrlMapping;
import io.listen.utils.ClientInfoUtils;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class UrlMappingService {

    @Inject
    ShortCodeGenerator shortCodeGenerator;

    @Inject
    ClientInfoUtils clientInfoUtils;


    @Inject
    @CacheName("short-link")
    Cache cache;

    @Context
    UriInfo uriInfo;

    public Uni<UrlMapping> createShortUrl(CreateShortUrlRequest createShortUrlRequest) {
        return UrlMapping.count("originalUrl = ?1 and userId = ?2", createShortUrlRequest.getOriginalUrl(),
                createShortUrlRequest.getUserId())
                .flatMap(count -> {
                    if (count > 0) {
                        return Uni.createFrom().failure(new RuntimeException("URL already exists"));
                    }
                    return Panache.withTransaction(() -> shortCodeGenerator.generateShortCode()
                            .flatMap(shortCode -> {
                                UrlMapping urlMapping = new UrlMapping();
                                urlMapping.shortCode = shortCode;
                                urlMapping.originalUrl = createShortUrlRequest.getOriginalUrl();
                                urlMapping.userId = Long.parseLong(createShortUrlRequest.getUserId());
                                urlMapping.title = createShortUrlRequest.getTitle();
                                urlMapping.description = createShortUrlRequest.getDescription();
                                urlMapping.expireTime = createShortUrlRequest.getExpireTime();
                                urlMapping.status = StatusEnum.NORMAL.value();
                                urlMapping.domain = uriInfo.getBaseUri().toString() + shortCode;
                                return urlMapping.<UrlMapping>persist()
                                        .invoke(res -> {
                                            cache.as(CaffeineCache.class).put(shortCode, CompletableFuture.completedFuture(res.originalUrl));
                                        });
                            }));
                })
                .onFailure()
                .invoke(e -> Log.errorf("Failed to create short URL: %s", e.getMessage()));
    }
    @WithTransaction
    public Uni<Void> recordClick(String shortCode, HttpServerRequest request) {
        // 第一步：更新 clickCount
        Uni<Integer> updateClickCount = UrlMapping
                .update("clickCount = clickCount + 1 where shortCode = ?1", shortCode);

        // 第二步：构建 ClickStatistic 实体
        ClickStatistic stats = new ClickStatistic();
        stats.shortCode = shortCode;
        stats.ipAddress = clientInfoUtils.getClientIpAddress(request);
        stats.userAgent = request.getHeader("User-Agent");
        stats.referer = request.getHeader("Referer");
        stats.clickTime = LocalDateTime.now();
        parseClientInfo(stats, stats.userAgent);

        // 第三步：执行更新和插入逻辑，组合为响应式事务
        return Panache.withTransaction(() ->
                updateClickCount
                        .flatMap(ignored -> stats.persist()) // persist() 返回 Uni<ClickStatistic>
                        .replaceWithVoid() // 最终返回 Uni<Void>
        );
    }

    private void parseClientInfo(ClickStatistic stats, String userAgent) {
        Map<String, String> clientInfo = clientInfoUtils.getClientInfo(userAgent);
        stats.browser = clientInfo.getOrDefault("client", "Unknown");
        stats.deviceType = clientInfo.getOrDefault("device", "Unknown");
        stats.os = clientInfo.getOrDefault("os", "Unknown");
        Map<String, String> ipInfo = clientInfoUtils.getIpInfo(stats.ipAddress);
        stats.country = ipInfo.getOrDefault("country", "Unknown");
        stats.city = ipInfo.getOrDefault("city", "Unknown");
    }


    public Uni<String> getOriginalUrl(String shortCode) {
        return cache.as(CaffeineCache.class).getAsync(shortCode, UrlMapping::findOriginalUrlByShortCode);
    }


}
