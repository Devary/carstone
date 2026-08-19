package org.devary.carstone.admin;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.devary.table.annotations.Searchable;
import org.devary.table.search.SearchContext;
import org.devary.table.utils.AnnotationSearchContextLoader;
import org.devary.table.utils.EntityScanner;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads every {@code @Searchable} entity found under {@code crud.entity-packages} at startup.
 * Ported from quar-crud-host verbatim (package renamed).
 */
@Startup
@ApplicationScoped
public class SearchContextRegistry {

    private final AnnotationSearchContextLoader loader = new AnnotationSearchContextLoader();
    private final EntityScanner scanner = new EntityScanner();

    private final Map<String, SearchContext> byEntityName = new LinkedHashMap<>();

    @ConfigProperty(name = "crud.entity-packages")
    List<String> entityPackages;

    @PostConstruct
    void init() {
        List<String> packages = entityPackages.stream().filter(p -> !p.isBlank()).toList();
        for (Class<?> entityClass : scanner.scan(Searchable.class, packages.toArray(new String[0]))) {
            SearchContext context = loader.load(entityClass);
            validate(entityClass.getName(), context);
            byEntityName.put(context.getName(), context);
        }
    }

    private void validate(String key, SearchContext context) {
        if (context.getName() == null || context.getName().isBlank()) {
            throw new IllegalStateException("Search context for '" + key + "' has no entity name");
        }
        if (context.getFields() == null || context.getFields().isEmpty()) {
            throw new IllegalStateException("Search context for '" + key + "' declares no fields");
        }
    }

    public Optional<SearchContext> byEntityName(String entityName) {
        return Optional.ofNullable(byEntityName.get(entityName));
    }
}
