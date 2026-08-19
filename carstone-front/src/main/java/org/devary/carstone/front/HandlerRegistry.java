package org.devary.carstone.front;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.devary.table.annotations.CrudstoneEntity;
import org.devary.table.annotations.CrudstoneResource;
import org.devary.table.crud.CrudHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Startup
@ApplicationScoped
public class HandlerRegistry {

    @Inject
    Instance<CrudHandler> handlers;

    private final Map<String, CrudHandler> byEntityName = new HashMap<>();

    @PostConstruct
    void init() {
        for (CrudHandler handler : handlers) {
            CrudstoneResource resource = handler.getClass().getAnnotation(CrudstoneResource.class);
            if (resource == null) {
                throw new IllegalStateException(
                        "CrudHandler " + handler.getClass().getName() + " is missing @CrudstoneResource");
            }
            CrudstoneEntity entity = resource.entity().getAnnotation(CrudstoneEntity.class);
            if (entity == null) {
                throw new IllegalStateException(
                        resource.entity().getName() + " is not annotated with @CrudstoneEntity");
            }
            byEntityName.put(entity.name(), handler);
        }
    }

    public Optional<CrudHandler> forEntity(String entityName) {
        return Optional.ofNullable(byEntityName.get(entityName));
    }
}
