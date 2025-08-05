package io.listen.auth;

import io.listen.exception.ServiceException;
import io.listen.model.User;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMechanism;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import java.util.Optional;

/**
 * 提供通过api key调用接口的能力
 */
@ApplicationScoped
@Priority(1)
@Alternative
public class ApiKeyAuthMechanism implements HttpAuthenticationMechanism {

    @Inject
    JWTAuthMechanism delegate;


    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        HttpServerRequest request = context.request();
        String apiKey = request.getHeader("Api-Key");
        if (apiKey == null) {
            return delegate.authenticate(context, identityProviderManager);
        }
        return Uni.createFrom().<Optional<User>>item(() -> User.find("apiKey = ?1 and status = 1", apiKey).firstResultOptional())
                .map(Unchecked.function(userOptional -> {
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        return (SecurityIdentity) QuarkusSecurityIdentity.builder()
                                .setPrincipal(new QuarkusPrincipal("user"))
                                .addRole(user.role)
                                .build();
                    }
                    throw new ServiceException("Invalid API Key");
                })).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }



    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return null;
    }
}
