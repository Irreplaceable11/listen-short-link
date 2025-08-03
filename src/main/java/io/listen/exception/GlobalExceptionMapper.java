package io.listen.exception;

import io.listen.dto.Result;
import io.listen.enums.ResponseStatusEnum;
import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(1)
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {


    @Override
    public Response toResponse(Throwable throwable) {
        Log.error(throwable.getMessage(), throwable);
        if (throwable instanceof ServiceException serviceException) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
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
