package io.listen.resource;

import io.listen.dto.Result;
import io.listen.dto.request.CreateUserRequest;
import io.listen.dto.request.UserLoginRequest;
import io.listen.service.UserService;
import io.quarkus.security.PermissionsAllowed;
import io.smallrye.mutiny.Uni;
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
    public Uni<Result<String>> login(@Valid UserLoginRequest userLoginRequest) {
        return userService.authenticate(userLoginRequest)
                .flatMap(val ->Uni.createFrom().item(Result.success(val)));
    }

    @POST
    @PermitAll
    @Path("/register")
    public Uni<Result<Void>> register(@Valid CreateUserRequest request) {
        return userService.register(request)
                .flatMap(unused ->Uni.createFrom().item(Result.success()));
    }



}
