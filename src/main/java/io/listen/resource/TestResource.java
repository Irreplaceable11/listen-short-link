package io.listen.resource;

import io.listen.generator.ShortCodeGenerator;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/test")
public class TestResource {

    @Inject
    ShortCodeGenerator shortCodeGenerator;

    @GET
    @Path("/id")
    public Uni<String> test() {
        return shortCodeGenerator.generateShortCode(); //TODO replace this stub to something useful
    }
}
