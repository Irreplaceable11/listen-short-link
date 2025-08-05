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
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

    @Transactional
    public UrlMapping createShortUrl(CreateShortUrlRequest createShortUrlRequest) {
        long count = UrlMapping.count("originalUrl = ?1 and userId = ?2", createShortUrlRequest.getOriginalUrl(),
                createShortUrlRequest.getUserId());
        if (count > 0) {
            throw new RuntimeException("url already exist");
        }
        String shortCode = shortCodeGenerator.generateShortCode();
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.shortCode = shortCode;
        urlMapping.originalUrl = createShortUrlRequest.getOriginalUrl();
        urlMapping.userId = Long.parseLong(createShortUrlRequest.getUserId());
        urlMapping.domain = createShortUrlRequest.getDomain();
        urlMapping.title = createShortUrlRequest.getTitle();
        urlMapping.description = createShortUrlRequest.getDescription();
        urlMapping.expireTime = createShortUrlRequest.getExpireTime();
        urlMapping.status = StatusEnum.NORMAL.value();
        urlMapping.domain = uriInfo.getBaseUri().toString() + shortCode;
        urlMapping.persistAndFlush();
        cache.as(CaffeineCache.class).put(shortCode, CompletableFuture.completedFuture(urlMapping.originalUrl));
        return urlMapping;
    }

    @Transactional
    public Uni<Void> recordClick(String shortCode, HttpServerRequest request) {
        return Uni.createFrom().<Void>item(() -> {
            UrlMapping.update("clickCount = clickCount + 1 where shortCode = ?1", shortCode);
            ClickStatistic stats = new ClickStatistic();
            stats.shortCode = shortCode;
            stats.ipAddress = clientInfoUtils.getClientIpAddress(request);
            stats.userAgent = request.getHeader("User-Agent");
            stats.referer = request.getHeader("Referer");
            stats.clickTime = LocalDateTime.now();
            // 解析设备信息、地理位置等
            parseClientInfo(stats, stats.userAgent);
            stats.persistAndFlush();
            return null;
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
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
        return cache.as(CaffeineCache.class).get(shortCode, UrlMapping::findOriginalUrlByShortCode);
    }


}
