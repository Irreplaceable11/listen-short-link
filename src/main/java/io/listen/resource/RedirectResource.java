package io.listen.resource;

import io.listen.service.UrlMappingService;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/")
public class RedirectResource {

    @Inject
    UrlMappingService urlMappingService;

    @Inject
    RoutingContext routingContext;

    @GET
    @Path("/{shortCode}")
    public void redirect(@PathParam("shortCode") String shortCode) {
        String originalUrl = urlMappingService.getOriginalUrl(shortCode).await().indefinitely();
        // 异步执行但不等待结果 (fire-and-forget)
        urlMappingService.recordClick(shortCode, routingContext.request())
                .subscribe().with(
                        result -> {}, // 成功时什么都不做
                        failure -> Log.error("Failed to record click", failure)
                );
        routingContext.redirect(originalUrl);

    }
}
