package org.devary.carstone.front;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.devary.carstone.domain.Seller;
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
@CrudstoneResource(entity = Seller.class)
public class SellerReadHandler implements CrudHandler {

    @Inject
    ObjectMapper mapper;

    @Override
    @WithSession
    public Uni<List<Map<String, Object>>> list() {
        return Seller.<Seller>listAll().map(sellers -> sellers.stream().map(this::toMap).toList());
    }

    @Override
    @WithSession
    public Uni<Optional<Map<String, Object>>> get(String id) {
        return Seller.<Seller>findById(Long.valueOf(id)).map(seller -> Optional.ofNullable(seller).map(this::toMap));
    }

    @Override
    public Uni<Map<String, Object>> create(Map<String, Object> body) {
        throw new ForbiddenException("carstone-front is read-only");
    }

    @Override
    public Uni<Optional<Map<String, Object>>> update(String id, Map<String, Object> body) {
        throw new ForbiddenException("carstone-front is read-only");
    }

    @Override
    public Uni<Boolean> delete(String id) {
        throw new ForbiddenException("carstone-front is read-only");
    }

    private static final Map<String, FieldKind> FIELD_KINDS = Map.of(
            "name", FieldKind.TEXT, "city", FieldKind.TEXT, "type", FieldKind.EXACT);
    private static final List<String> SEARCH_FIELDS = List.of("name", "city", "email");

    @Override
    @WithSession
    public Uni<PagedResult> query(ListQuery query) {
        Built built = PanacheListQuerySupport.build(query, FIELD_KINDS, SEARCH_FIELDS);
        var panacheQuery = built.hasWhere()
                ? Seller.<Seller>find(built.where(), built.sort(), built.params())
                : Seller.<Seller>findAll(built.sort());
        panacheQuery.page(Page.of(query.from() / query.pageSize(), query.pageSize()));
        Uni<Long> count = built.hasWhere() ? Seller.count(built.where(), built.params()) : Seller.count();
        return Uni.combine().all().unis(count, panacheQuery.list())
                .asTuple()
                .map(t -> new PagedResult(t.getItem1(), t.getItem2().stream().map(this::toMap).toList()));
    }

    private Map<String, Object> toMap(Seller seller) {
        Map<String, Object> map = mapper.convertValue(seller, new TypeReference<Map<String, Object>>() {
        });
        map.put("id", String.valueOf(seller.id));
        return map;
    }
}
