package org.devary.carstone.gateway.registry;

import org.devary.carstone.gateway.config.GatewayServicesConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConfigBackedServiceRegistry implements ServiceRegistry {

    private final GatewayServicesConfig config;

    @Override
    public List<RegisteredService> services() {
        return config.services().entrySet().stream()
                .map(this::toRegisteredService)
                .toList();
    }

    @Override
    public Optional<RegisteredService> findService(String serviceName) {
        return Optional.ofNullable(config.services().get(serviceName))
                .map(service -> new RegisteredService(serviceName, service.baseUrl()));
    }

    private RegisteredService toRegisteredService(Map.Entry<String, GatewayServicesConfig.ServiceConfig> entry) {
        return new RegisteredService(entry.getKey(), entry.getValue().baseUrl());
    }
}
