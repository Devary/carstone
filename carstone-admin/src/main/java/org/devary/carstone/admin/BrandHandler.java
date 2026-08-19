package org.devary.carstone.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.devary.carstone.domain.Brand;
import org.devary.carstone.domain.support.PanacheListQuerySupport;
import org.devary.carstone.domain.support.PanacheListQuerySupport.Built;
import org.devary.carstone.domain.support.PanacheListQuerySupport.FieldKind;
import org.devary.table.annotations.CrudstoneResource;
import org.devary.table.crud.CrudHandler;
import org.devary.table.crud.ListQuery;
import org.devary.table.crud.PagedResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@CrudstoneResource(entity = Brand.class)
public class BrandHandler implements CrudHandler {

    @Inject
    ObjectMapper mapper;

    @Override
    @WithSession
    public Uni<List<Map<String, Object>>> list() {
        return Brand.<Brand>listAll().map(brands -> brands.stream().map(this::toMap).toList());
    }

    @Override
    @WithSession
    public Uni<Optional<Map<String, Object>>> get(String id) {
        return Brand.<Brand>findById(Long.valueOf(id)).map(brand -> Optional.ofNullable(brand).map(this::toMap));
    }

    @Override
    @WithTransaction
    public Uni<Map<String, Object>> create(Map<String, Object> body) {
        Brand brand = new Brand();
        apply(brand, body);
        return brand.<Brand>persist().map(this::toMap);
    }

    @Override
    @WithTransaction
    public Uni<Optional<Map<String, Object>>> update(String id, Map<String, Object> body) {
        return Brand.<Brand>findById(Long.valueOf(id)).map(brand -> {
            if (brand == null) {
                return Optional.<Map<String, Object>>empty();
            }
            apply(brand, body);
            return Optional.of(toMap(brand));
        });
    }

    @Override
    @WithTransaction
    public Uni<Boolean> delete(String id) {
        return Brand.deleteById(Long.valueOf(id));
    }

    private static final Map<String, FieldKind> FIELD_KINDS = Map.of(
            "name", FieldKind.TEXT, "country", FieldKind.TEXT);
    private static final List<String> SEARCH_FIELDS = List.of("name", "country");

    @Override
    @WithSession
    public Uni<PagedResult> query(ListQuery query) {
        Built built = PanacheListQuerySupport.build(query, FIELD_KINDS, SEARCH_FIELDS);
        var panacheQuery = built.hasWhere()
                ? Brand.<Brand>find(built.where(), built.sort(), built.params())
                : Brand.<Brand>findAll(built.sort());
        panacheQuery.page(Page.of(query.from() / query.pageSize(), query.pageSize()));
        Uni<Long> count = built.hasWhere() ? Brand.count(built.where(), built.params()) : Brand.count();
        return Uni.combine().all().unis(count, panacheQuery.list())
                .asTuple()
                .map(t -> new PagedResult(t.getItem1(), t.getItem2().stream().map(this::toMap).toList()));
    }

    private void apply(Brand brand, Map<String, Object> body) {
        try {
            mapper.updateValue(brand, body);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid brand payload", e);
        }
    }

    private Map<String, Object> toMap(Brand brand) {
        Map<String, Object> map = mapper.convertValue(brand, new TypeReference<Map<String, Object>>() {
        });
        map.put("id", String.valueOf(brand.id));
        return map;
    }
}
