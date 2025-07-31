package io.listen.resource;

import io.listen.config.SnowflakeConfig;
import io.listen.generator.ShortCodeGenerator;
import io.listen.generator.SnowflakeIdGenerator;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("")
public class TestResource {

    @Inject
    ShortCodeGenerator shortCodeGenerator;

    @GET
    @Path("/id")
    public String test() {
        return shortCodeGenerator.generateShortCode(); //TODO replace this stub to something useful
    }
}
