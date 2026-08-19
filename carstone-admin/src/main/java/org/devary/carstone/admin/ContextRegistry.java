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
    // keyed by CrudstoneEntity#path() (falling back to name() when path is blank, same default
    // CrudstoneEntity itself documents) — REQUIRED, not just a nice-to-have: crudstone's own
    // EntityPageComponent/EntityService request `GET /{routeParam}?...&includeContext=true`
    // using the RAW route segment directly (never a separate /context/{name} lookup first), and
    // for CarListing that route segment is "listings" (its path), not "carListings" (its name)
    // — confirmed live via a failing Cypress run before this fix existed (a 404, not a config
    // typo) — every other carstone entity's path happens to equal its name, which is exactly why
    // this gap went unnoticed until an entity with a genuinely different one existed.
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

    // resolves either form to the entity's real context: its own name, or the route path a
    // sidebar link/browser URL actually uses (see byPath's own comment for why both are needed)
    public Optional<TableContext> byKeyOrEntityName(String name) {
        Optional<TableContext> byName = byEntityName(name);
        return byName.isPresent() ? byName : Optional.ofNullable(byPath.get(name));
    }

    public Set<String> keys() {
        return byEntityName.keySet();
    }
}
