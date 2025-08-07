package io.listen.service;

import io.listen.dto.request.CreateUserRequest;
import io.listen.dto.request.UserLoginRequest;
import io.listen.enums.StatusEnum;
import io.listen.exception.ServiceException;
import io.listen.model.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class UserService {

    public String authenticate(UserLoginRequest userLoginRequest) {
//        Optional<User> userOptional = User.find("username", userLoginRequest.getUsername()).firstResultOptional();
//        if (userOptional.isEmpty()) {
//            throw new ServiceException("user not found");
//        }
//        User user = userOptional.get();
//        if (!BcryptUtil.matches(userLoginRequest.getPassword(), user.password)) {
//            throw new ServiceException("username or password incorrect");
//        }
//        return Jwt.subject(user.username)
//                .groups(Set.of("user")) // 可选：设置用户角色
//                .expiresIn(Duration.ofHours(1)) // 有效期 1 小时
//                .sign();// 使用配置的私钥签名
        return "";
    }

    @Transactional
    public void register(CreateUserRequest request) {
//        Optional<User> userOptional = User.find("email", request.getEmail()).firstResultOptional();
//        if (userOptional.isPresent()) {
//            throw new ServiceException("email already exists");
//        }
//        User user = new User();
//        user.email = request.getEmail();
//        user.password = BcryptUtil.bcryptHash(request.getPassword());
//        user.username = request.getUsername();
//        user.status = StatusEnum.NORMAL.value();
//        user.persistAndFlush();
    }
}
