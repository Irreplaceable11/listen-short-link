package io.listen.service;

import io.listen.dto.request.CreateUserRequest;
import io.listen.dto.request.UserLoginRequest;
import io.listen.enums.StatusEnum;
import io.listen.exception.ServiceException;
import io.listen.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class UserService {

    @WithTransaction
    public Uni<String> authenticate(UserLoginRequest userLoginRequest) {
        return User.find("email", userLoginRequest.getUsername())
                .<User>singleResult()
                .onItem().ifNull().failWith(() -> new WebApplicationException("Invalid credentials"))
                .onItem().call(user -> BcryptUtil.matches(userLoginRequest.getPassword(), user.password)
                        ? Uni.createFrom().voidItem()
                        : Uni.createFrom().failure(new ServiceException("username or password is incorrect")))
                .map(user -> {
                    return Jwt.subject(String.valueOf(user.id))
                            .groups(Set.of("user")) // 可选：设置用户角色
                            .expiresIn(Duration.ofHours(1)) // 有效期 1 小时
                            .sign();// 使用配置的私钥签名

                });
    }

    @WithTransaction
    public Uni<Void> register(CreateUserRequest request) {
        return User.find("email", request.getEmail())
                .<User>firstResult()
                .onItem().ifNotNull().failWith(new ServiceException("Email already exists"))
                .chain(() -> {
                    User user = new User();
                    user.email = request.getEmail();
                    user.username = request.getUsername();
                    user.password = BcryptUtil.bcryptHash(request.getPassword()); // 加密密码
                    user.status = StatusEnum.NORMAL.value(); // 假设 StatusEnum 已定义
                    // 持久化并刷新
                    return user.persistAndFlush().replaceWithVoid();
                });
    }
}
