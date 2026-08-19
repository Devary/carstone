package org.devary.carstone.admin;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.devary.table.annotations.Sidebar;
import org.devary.table.sidebar.SidebarContext;
import org.devary.table.utils.AnnotationSidebarLoader;
import org.devary.table.utils.EntityScanner;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads every {@code @Sidebar} class found under {@code crud.entity-packages} at startup.
 * Ported from quar-crud-host verbatim (package renamed).
 */
@Startup
@ApplicationScoped
public class SidebarRegistry {

    private final AnnotationSidebarLoader loader = new AnnotationSidebarLoader();
    private final EntityScanner scanner = new EntityScanner();

    private final Map<String, SidebarContext> byName = new LinkedHashMap<>();

    @ConfigProperty(name = "crud.entity-packages")
    List<String> entityPackages;

    @PostConstruct
    void init() {
        List<String> packages = entityPackages.stream().filter(p -> !p.isBlank()).toList();
        for (Class<?> sidebarClass : scanner.scan(Sidebar.class, packages.toArray(new String[0]))) {
            SidebarContext context = loader.load(sidebarClass);
            validate(sidebarClass.getName(), context);
            byName.put(context.getName(), context);
        }
    }

    private void validate(String key, SidebarContext context) {
        if (context.getName() == null || context.getName().isBlank()) {
            throw new IllegalStateException("Sidebar for '" + key + "' has no name");
        }
        if (context.getNodes() == null || context.getNodes().isEmpty()) {
            throw new IllegalStateException("Sidebar for '" + key + "' declares no nodes");
        }
    }

    public Optional<SidebarContext> byName(String name) {
        return Optional.ofNullable(byName.get(name));
    }
}
