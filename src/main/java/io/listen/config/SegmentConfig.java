package io.listen.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "short-link.segment")
public interface SegmentConfig {

    Long initialValue();

    Integer step();

    Integer prefetchThreshold();
}
