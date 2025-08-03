package io.listen.exception;

import io.listen.dto.Result;
import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class UnauthorizedFailedExceptionMapper implements ExceptionMapper<UnauthorizedException> {
    @Override
    public Response toResponse(UnauthorizedException e) {
        return Response.status(200).entity(Result.failure("Authentication failed")).build();
    }
}
