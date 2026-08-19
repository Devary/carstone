package org.devary.carstone.admin;

import jakarta.enterprise.context.ApplicationScoped;
import org.devary.table.crud.InMemoryQueryEngine;
import org.devary.table.crud.ListQuery;
import org.devary.table.crud.PagedResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic EntityStore-backed fallback {@link EntityResource} calls when an entity has no
 * registered {@link HandlerRegistry} override — ported verbatim from quar-crud-host as a
 * defensive fallback, kept as dead code in practice: every carstone entity (Brand/Seller/
 * CarListing) has a real Panache-backed handler, so this never actually gets exercised.
 * Minimizes deviation from proven reference code rather than restructuring EntityResource to
 * assume a handler always exists.
 */
@ApplicationScoped
public class EntityStore {

    private final Map<String, Map<String, Map<String, Object>>> data = new ConcurrentHashMap<>();

    private Map<String, Map<String, Object>> table(String entity) {
        return data.computeIfAbsent(entity, k -> Collections.synchronizedMap(new LinkedHashMap<>()));
    }

    public List<Map<String, Object>> all(String entity) {
        synchronized (table(entity)) {
            return new ArrayList<>(table(entity).values());
        }
    }

    public Optional<Map<String, Object>> one(String entity, String id) {
        return Optional.ofNullable(table(entity).get(id));
    }

    public Map<String, Object> create(String entity, Map<String, Object> body) {
        Map<String, Object> entry = new LinkedHashMap<>(body);
        String id = UUID.randomUUID().toString();
        entry.put("id", id);
        table(entity).put(id, entry);
        return entry;
    }

    public Optional<Map<String, Object>> update(String entity, String id, Map<String, Object> body) {
        if (!table(entity).containsKey(id)) {
            return Optional.empty();
        }
        Map<String, Object> entry = new LinkedHashMap<>(body);
        entry.put("id", id);
        table(entity).put(id, entry);
        return Optional.of(entry);
    }

    public boolean delete(String entity, String id) {
        return table(entity).remove(id) != null;
    }

    public PagedResult query(String entity, ListQuery query) {
        return InMemoryQueryEngine.apply(all(entity), query);
    }

    public int deleteMany(String entity, List<String> ids) {
        Map<String, Map<String, Object>> t = table(entity);
        int removed = 0;
        synchronized (t) {
            for (String id : ids) {
                if (t.remove(id) != null) {
                    removed++;
                }
            }
        }
        return removed;
    }
}
