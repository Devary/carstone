package org.devary.carstone.gateway.registry;

import java.util.List;
import java.util.Optional;

public interface ServiceRegistry {

    List<RegisteredService> services();

    Optional<RegisteredService> findService(String serviceName);
}
