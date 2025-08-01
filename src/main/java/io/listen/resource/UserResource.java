package io.listen.resource;

import io.listen.dto.Result;
import io.listen.dto.request.CreateUserRequest;
import io.listen.dto.request.UserLoginRequest;
import io.listen.service.UserService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/user")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class UserResource {

    @Inject
    UserService userService;

    @POST
    @Path("/login")
    @PermitAll
    public Result<String> login(@Valid UserLoginRequest userLoginRequest) {
        return Result.success(userService.authenticate(userLoginRequest));
    }

    @POST
    @PermitAll
    @Path("/register")
    public Result<String> register(@Valid CreateUserRequest request) {
        userService.register(request);
        return Result.success(null);
    }



}
