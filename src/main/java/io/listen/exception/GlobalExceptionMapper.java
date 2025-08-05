package io.listen.exception;

import io.listen.dto.Result;
import io.quarkus.logging.Log;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.net.URI;
import java.net.URISyntaxException;

@Provider
public class GlobalExceptionMapper  {


    @ServerExceptionMapper
    public Response handlerNotFoundException(NotFoundException e, @Context UriInfo uriInfo) throws URISyntaxException {
        return Response.temporaryRedirect(new URI(uriInfo.getBaseUri().toString() + "q/dev-ui/welcome")).build();
    }

    //没有jwt时访问端口触发这个
    @ServerExceptionMapper
    public Response toResponse(UnauthorizedException e) {
        return Response.status(200).entity(Result.failure("Authentication failed")).build();
    }


    @ServerExceptionMapper
    public Response toResponse(Throwable throwable) {
        Log.error(throwable.getMessage(), throwable);
        if (throwable instanceof ServiceException serviceException) {
            return Response
                    .status(Response.Status.OK)
                    .entity(Result.failure(serviceException.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Result.failure("system error"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
