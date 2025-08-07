package io.listen.resource;

import io.listen.service.UrlMappingService;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/")
public class RedirectResource {

    @Inject
    UrlMappingService urlMappingService;

    @Inject
    RoutingContext routingContext;

    @POST
    @Path("/{shortCode}")
    public Uni<Void> redirect(@PathParam("shortCode") String shortCode) {
        return urlMappingService.getOriginalUrl(shortCode)
                .flatMap(url -> {
                    routingContext.redirect(url);
                    return urlMappingService.recordClick(shortCode, routingContext.request());
                })
                .onFailure().invoke(failure -> {
                    // 记录失败日志
                    Log.error("Failed to process redirect or record click", failure);
                    routingContext.response()
                            .setStatusCode(500)
                            .end("Internal Server Error");
                });
    }
}
