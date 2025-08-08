package io.listen.resource;

import io.listen.service.UrlMappingService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.UriInfo;

@Path("/")
public class RedirectResource {

    @Inject
    UrlMappingService urlMappingService;

    @Inject
    RoutingContext routingContext;

    @GET
    @Path("/{shortCode: [a-zA-Z0-9]{6,8}}")
    public Uni<Void> redirect(@PathParam("shortCode") String shortCode, UriInfo uriInfo) {
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
