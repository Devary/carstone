package org.devary.carstone.domain.support;

import java.util.List;
import java.util.Map;

/**
 * Fixes one thing a plain {@code mapper.convertValue(entity, Map.class)} gets wrong for a
 * relation embedded in a handler's own {@code toMap} — Jackson serializes a real relation's
 * nested {@code id} as whatever raw type the entity declares it as (a {@code Long}, via
 * {@code PanacheEntity}), but every OTHER id in this API is a String (a handler's own
 * {@code toMap} always does {@code map.put("id", String.valueOf(entity.id))} for the row's own
 * top-level id). Left alone, that mismatch breaks the frontend's id-based matching of a selected
 * relation option against its own fetched list (PrimeNG's dataKey comparison uses {@code ===},
 * so {@code 1 !== "1"}).
 *
 * <p>Ported from {@code quar-crud-host}'s own {@code RelationIds} — carstone has no soft
 * JSON-text-list relations (unlike Studio.titles/StudioTitle.genres there), so the
 * JSON_TEXT_LIST_FIELDS special-casing that class needed is dropped here; only the nested-id
 * stringification is needed for Brand/Seller embedded inside CarListing's own toMap.
 */
public final class RelationIds {

    private RelationIds() {
    }

    @SuppressWarnings("unchecked")
    public static void stringifyNestedIds(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                fixAndRecurse((Map<String, Object>) nested);
            } else if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> itemMap) {
                        fixAndRecurse((Map<String, Object>) itemMap);
                    }
                }
            }
        }
    }

    private static void fixAndRecurse(Map<String, Object> nested) {
        Object id = nested.get("id");
        if (id != null && !(id instanceof String)) {
            nested.put("id", String.valueOf(id));
        }
        stringifyNestedIds(nested);
    }
}
