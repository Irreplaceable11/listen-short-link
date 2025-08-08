package io.listen.config;

import io.listen.generator.ShortCodeGenerator;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.VertxContextSupport;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class StartupInitializer {

    @Inject
    ShortCodeGenerator shortCodeGenerator;

    void onStartup(@Observes StartupEvent ev) throws Throwable {
        VertxContextSupport.subscribeAndAwait(() ->
                shortCodeGenerator.ensureInitialized()
                .invoke(() -> Log.info("Short code generator has been initialized"))
                .onFailure()
                .invoke(e-> Log.error("Short code generator has not been initialized", e)));
    }
}
