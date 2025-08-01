package io.listen.resource;

import io.listen.dto.Result;
import io.listen.dto.request.CreateShortUrlRequest;
import io.listen.service.UrlMappingService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/urls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class UrlResource {

    @Inject
    UrlMappingService urlMappingService;


    @POST
    @RolesAllowed({"user"})
    public Result<String> createShortUrl(@Valid CreateShortUrlRequest createShortUrlRequest) {
        return Result.success("success");
    }
}
