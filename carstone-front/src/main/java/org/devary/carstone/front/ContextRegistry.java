package org.devary.carstone.front;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.devary.table.TableContext;
import org.devary.table.utils.AnnotationContextLoader;
import org.devary.table.utils.EntityScanner;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Startup
@ApplicationScoped
public class ContextRegistry {

    private final AnnotationContextLoader annotationLoader = new AnnotationContextLoader();
    private final EntityScanner scanner = new EntityScanner();

    private final Map<String, TableContext> byEntityName = new LinkedHashMap<>();
    // see carstone-admin's own ContextRegistry for why this is required, not optional — a
    // route/sidebar-link path can genuinely differ from the entity's own name (CarListing:
    // name="carListings", path="listings"), and crudstone's frontend requests by the raw route
    // segment, never a resolved entity name
    private final Map<String, TableContext> byPath = new LinkedHashMap<>();

    @ConfigProperty(name = "crud.entity-packages")
    List<String> entityPackages;

    @PostConstruct
    void init() {
        List<String> packages = entityPackages.stream().filter(p -> !p.isBlank()).toList();
        for (Class<?> entityClass : scanner.scan(packages.toArray(new String[0]))) {
            TableContext context = annotationLoader.load(entityClass);
            validate(entityClass.getName(), context);
            byEntityName.put(context.getName(), context);
            String path = context.getPath() == null || context.getPath().isBlank() ? context.getName() : context.getPath();
            byPath.put(path, context);
        }
    }

    private void validate(String key, TableContext context) {
        if (context.getName() == null || context.getName().isBlank()) {
            throw new IllegalStateException("Context '" + key + "' has no entity name");
        }
        if (context.getFields() == null || context.getFields().isEmpty()) {
            throw new IllegalStateException("Context '" + key + "' declares no fields");
        }
    }

    public boolean hasEntity(String entityName) {
        return byEntityName.containsKey(entityName);
    }

    public Optional<TableContext> byEntityName(String entityName) {
        return Optional.ofNullable(byEntityName.get(entityName));
    }

    public Optional<TableContext> byKeyOrEntityName(String name) {
        Optional<TableContext> byName = byEntityName(name);
        return byName.isPresent() ? byName : Optional.ofNullable(byPath.get(name));
    }

    public Set<String> keys() {
        return byEntityName.keySet();
    }
}
