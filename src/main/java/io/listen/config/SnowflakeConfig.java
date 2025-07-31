package io.listen.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "snowflake")
public interface SnowflakeConfig {

    Long workerId();

    Long datacenterId();
}
