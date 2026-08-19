package org.devary.carstone.domain.support;

import io.quarkus.panache.common.Sort;
import org.devary.table.crud.ListQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a generic {@link ListQuery} into the pieces a Panache reactive active-record query
 * needs: an HQL where-clause, its params, and a {@link Sort}. Doesn't run the query itself —
 * Panache's static finder methods only work called directly on the concrete entity class
 * (Quarkus rewrites them at build time), so each handler calls those itself using the pieces
 * this builds.
 *
 * <p>Ported from {@code quar-crud-host}'s own {@code PanacheListQuerySupport} (same shape,
 * same "each handler describes its own entity via a FieldKind per filterable field name"
 * design) with one addition: {@link FieldKind#RANGE_MIN}/{@link FieldKind#RANGE_MAX}, the
 * backend half of context-gen's new {@code SearchableField#rangeTarget} mechanism — a wire
 * field name like {@code "priceMin"} doesn't name a real column itself, it bounds a DIFFERENT
 * one (via {@code rangeTargetColumns}) with {@code >=}/{@code <=} instead of the usual
 * contains/exact/in-list matching the other kinds do.
 */
public final class PanacheListQuerySupport {

    /** How a field should be matched — decides the HQL fragment {@link #build} generates for it. */
    public enum FieldKind {
        /** A free-text column: a single filter value is a CONTAINS match. */
        TEXT,
        /** A short exact-value scalar column (an enum) — {@code field IN (:vals)}. */
        EXACT,
        /** A {@code @ManyToOne} relation: filtered/matched on the target's own id. */
        RELATION_ID,
        /**
         * A virtual "from"/min bound on a DIFFERENT field's own column (named via
         * {@code rangeTargetColumns}), not a filter on its own value — {@code target >= :value}.
         * Wire field name doesn't have to (and for a range field, doesn't) match a real column.
         */
        RANGE_MIN,
        /** Same as {@link #RANGE_MIN}, the "to"/max bound — {@code target <= :value}. */
        RANGE_MAX,
    }

    public record Built(String where, Map<String, Object> params, Sort sort) {
        public boolean hasWhere() {
            return where != null && !where.isEmpty();
        }
    }

    private PanacheListQuerySupport() {
    }

    public static Built build(ListQuery query, Map<String, FieldKind> fieldKinds, List<String> searchFields) {
        return build(query, fieldKinds, searchFields, Map.of());
    }

    public static Built build(ListQuery query, Map<String, FieldKind> fieldKinds, List<String> searchFields,
                               Map<String, String> rangeTargetColumns) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (query.hasSearch() && !searchFields.isEmpty()) {
            String needle = "%" + query.q().toLowerCase() + "%";
            List<String> ors = new ArrayList<>();
            for (int i = 0; i < searchFields.size(); i++) {
                String param = "search" + i;
                ors.add("lower(" + searchFields.get(i) + ") like :" + param);
                params.put(param, needle);
            }
            conditions.add("(" + String.join(" or ", ors) + ")");
        }

        int filterIndex = 0;
        for (Map.Entry<String, List<String>> filter : query.filters().entrySet()) {
            FieldKind kind = fieldKinds.get(filter.getKey());
            if (kind == null || filter.getValue().isEmpty()) {
                continue; // unknown/unfiltered field name — ignore rather than 500 on a stale UI param
            }
            String param = "filter" + (filterIndex++);
            String condition = buildCondition(filter.getKey(), kind, filter.getValue(), param, params, rangeTargetColumns);
            if (condition != null) {
                conditions.add(condition);
            }
        }

        String where = conditions.isEmpty() ? "" : String.join(" and ", conditions);
        Sort sort = query.hasSort() && fieldKinds.containsKey(query.sortField())
                ? Sort.by(query.sortField(), query.sortDesc() ? Sort.Direction.Descending : Sort.Direction.Ascending)
                : Sort.by("id");
        return new Built(where, params, sort);
    }

    private static String buildCondition(String field, FieldKind kind, List<String> values, String param,
                                          Map<String, Object> params, Map<String, String> rangeTargetColumns) {
        boolean empty = ListQuery.isEmptyMatch(values);
        return switch (kind) {
            case TEXT -> {
                if (empty) {
                    yield field + " is null";
                }
                List<String> ors = new ArrayList<>();
                for (int i = 0; i < values.size(); i++) {
                    String p = param + "_" + i;
                    params.put(p, "%" + values.get(i).toLowerCase() + "%");
                    ors.add("lower(" + field + ") like :" + p);
                }
                yield "(" + String.join(" or ", ors) + ")";
            }
            case EXACT -> {
                if (empty) {
                    yield field + " is null";
                }
                params.put(param, values);
                yield field + " in (:" + param + ")";
            }
            case RELATION_ID -> {
                if (empty) {
                    yield field + " is null";
                }
                params.put(param, values.stream().map(Long::valueOf).toList());
                yield field + ".id in (:" + param + ")";
            }
            case RANGE_MIN -> buildRangeCondition(field, ">=", values.get(0), param, params, rangeTargetColumns);
            case RANGE_MAX -> buildRangeCondition(field, "<=", values.get(0), param, params, rangeTargetColumns);
        };
    }

    // unparseable/malformed bound value -> skip the condition silently rather than 500, matching
    // this class's existing "unknown field name -> ignore rather than fail" philosophy for
    // anything that could be a stale/hand-crafted UI param
    private static String buildRangeCondition(String field, String operator, String rawValue, String param,
                                               Map<String, Object> params, Map<String, String> rangeTargetColumns) {
        String column = rangeTargetColumns.getOrDefault(field, field);
        try {
            params.put(param, Double.valueOf(rawValue));
            return column + " " + operator + " :" + param;
        } catch (NumberFormatException notANumber) {
            try {
                params.put(param, java.time.LocalDate.parse(rawValue));
                return column + " " + operator + " :" + param;
            } catch (Exception notADateEither) {
                return null;
            }
        }
    }
}
