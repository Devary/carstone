package org.devary.carstone.admin;

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

/**
 * Loads every entity found under {@code crud.entity-packages} (annotation scan) at startup,
 * failing fast on anything broken. carstone has no YAML-backed contexts (unlike quar-crud-host's
 * anime/sharacter/faq) — every entity is a real Panache-backed one with its own CrudHandler.
 * Ported from quar-crud-host, YAML-loading branch dropped.
 */
@Startup
@ApplicationScoped
public class ContextRegistry {

    private final AnnotationContextLoader annotationLoader = new AnnotationContextLoader();
    private final EntityScanner scanner = new EntityScanner();

    private final Map<String, TableContext> byEntityName = new LinkedHashMap<>();

    @ConfigProperty(name = "crud.entity-packages")
    List<String> entityPackages;

    @PostConstruct
    void init() {
        List<String> packages = entityPackages.stream().filter(p -> !p.isBlank()).toList();
        for (Class<?> entityClass : scanner.scan(packages.toArray(new String[0]))) {
            TableContext context = annotationLoader.load(entityClass);
            validate(entityClass.getName(), context);
            byEntityName.put(context.getName(), context);
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

    // carstone has no YAML route-key aliasing (every entity's route key IS its entity name), but
    // EntityResource still calls this generic name to keep its own code identical to
    // quar-crud-host's — here it's just a synonym for byEntityName
    public Optional<TableContext> byKeyOrEntityName(String name) {
        return byEntityName(name);
    }

    public Set<String> keys() {
        return byEntityName.keySet();
    }
}
