package io.listen.config;

import io.listen.generator.ShortCodeGenerator;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class StartupInitializer {

    @Inject
    ShortCodeGenerator shortCodeGenerator;

    void onStartup(@Observes StartupEvent ev, Vertx vertx) throws Throwable {
        VertxContextSupport.subscribeAndAwait(() -> {
            return shortCodeGenerator.ensureInitialized()
                    .invoke(() -> {Log.info("init =======");})
                    .onFailure()
                    .invoke(()->{Log.info("init fail ========");});
//                    .with(success -> {
//                        Log.info("Short code generator has been initialized");
//                    }, failure -> {
//                        Log.info("Short code generator has not been initialized");
//                    });
        });
    }
}
