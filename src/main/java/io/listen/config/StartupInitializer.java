package io.listen.config;

import io.listen.generator.ShortCodeGenerator;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class StartupInitializer {

    @Inject
    ShortCodeGenerator shortCodeGenerator;

    @Inject
    Vertx vertx;

    void onStartup(@Observes StartupEvent ev) throws Throwable {
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
        VertxContextSafetyToggle.setContextSafe(context, true);
        context.runOnContext(v -> {

            shortCodeGenerator.ensureInitialized()
                    .invoke(() -> Log.info("Short code generator has been initialized"))
                    .subscribe()
                    .with(
                            result -> Log.info("Database initialized successfully"),
                            failure -> Log.error("Failed to initialize database", failure)
                    );
        });
//        VertxContextSupport.subscribeAndAwait(() ->
//                shortCodeGenerator.ensureInitialized()
//                .invoke(() -> Log.info("Short code generator has been initialized"))
//                .onFailure()
//                .invoke(e-> Log.error("Short code generator has not been initialized", e)));
    }
}
