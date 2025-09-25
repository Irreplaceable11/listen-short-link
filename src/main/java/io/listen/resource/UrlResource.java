package io.listen.resource;

import io.listen.dto.Result;
import io.listen.dto.request.CreateShortUrlRequest;
import io.listen.model.UrlMapping;
import io.listen.service.UrlMappingService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.ArrayList;
import java.util.List;

@Path("/urls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class UrlResource {

    @Inject
    UrlMappingService urlMappingService;


    @POST
    @RolesAllowed({"user"})
    public Uni<Result<UrlMapping>> createShortUrl(@Valid CreateShortUrlRequest createShortUrlRequest, @Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();
        return urlMappingService.createShortUrl(createShortUrlRequest, Long.parseLong(userId))
                .flatMap(result -> Uni.createFrom().item(Result.success(result)));
    }

    @GET
    @Path("my")
    public Uni<Result<List<UrlMapping>>> getAllShortUrls(@Context SecurityContext ctx) {
        String id = ctx.getUserPrincipal().getName();
        return urlMappingService.list(Long.parseLong(id))
                .map(Result::success);
    }
}
