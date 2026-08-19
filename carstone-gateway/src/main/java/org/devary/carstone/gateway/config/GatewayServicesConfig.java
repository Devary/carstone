package org.devary.carstone.gateway.config;

import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigMapping(prefix = "gateway")
public interface GatewayServicesConfig {

    Map<String, ServiceConfig> services();

    interface ServiceConfig {
        String baseUrl();
    }
}
