package io.listen.service;

import io.listen.dto.request.CreateShortUrlRequest;
import io.listen.enums.StatusEnum;
import io.listen.generator.ShortCodeGenerator;
import io.listen.model.UrlMapping;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
@Transactional
public class UrlMappingService {

    @Inject
    ShortCodeGenerator shortCodeGenerator;

    @Inject
    @CacheName("short-link")
    Cache cache;

    @Context
    UriInfo uriInfo;

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
        Uni<UrlMapping> urlMappingUni = cache.get(shortCode, k -> urlMapping);
        return urlMappingUni.await().indefinitely();
    }
}
