package org.devary.carstone.front;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.devary.carstone.domain.CarListing;
import org.devary.carstone.domain.support.PanacheListQuerySupport;
import org.devary.carstone.domain.support.PanacheListQuerySupport.Built;
import org.devary.carstone.domain.support.PanacheListQuerySupport.FieldKind;
import org.devary.carstone.domain.support.RelationIds;
import org.devary.table.annotations.CrudstoneResource;
import org.devary.table.crud.CrudHandler;
import org.devary.table.crud.ListQuery;
import org.devary.table.crud.PagedResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The entity carstone-front-ui's search UI actually calls. Same FIELD_KINDS/RANGE_TARGET_COLUMNS
 * shape as carstone-admin's own CarListingHandler (must stay in sync — both read the same
 * columns), minus every write path.
 */
@ApplicationScoped
@CrudstoneResource(entity = CarListing.class)
public class CarListingReadHandler implements CrudHandler {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    @Inject
    ObjectMapper mapper;

    @Override
    @WithSession
    public Uni<List<Map<String, Object>>> list() {
        return CarListing.<CarListing>listAll().map(listings -> listings.stream().map(this::toMap).toList());
    }

    @Override
    @WithSession
    public Uni<Optional<Map<String, Object>>> get(String id) {
        return CarListing.<CarListing>findById(Long.valueOf(id))
                .map(listing -> Optional.ofNullable(listing).map(this::toMap));
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

    private static final Map<String, FieldKind> FIELD_KINDS = Map.ofEntries(
            Map.entry("title", FieldKind.TEXT), Map.entry("model", FieldKind.TEXT),
            Map.entry("city", FieldKind.TEXT),
            Map.entry("brand", FieldKind.RELATION_ID), Map.entry("seller", FieldKind.RELATION_ID),
            Map.entry("fuelType", FieldKind.EXACT), Map.entry("transmission", FieldKind.EXACT),
            Map.entry("bodyType", FieldKind.EXACT), Map.entry("color", FieldKind.EXACT),
            Map.entry("sellerType", FieldKind.EXACT),
            Map.entry("yearMin", FieldKind.RANGE_MIN), Map.entry("yearMax", FieldKind.RANGE_MAX),
            Map.entry("priceMin", FieldKind.RANGE_MIN), Map.entry("priceMax", FieldKind.RANGE_MAX),
            Map.entry("mileageMin", FieldKind.RANGE_MIN), Map.entry("mileageMax", FieldKind.RANGE_MAX));
    private static final Map<String, String> RANGE_TARGET_COLUMNS = Map.of(
            "yearMin", "year", "yearMax", "year",
            "priceMin", "price", "priceMax", "price",
            "mileageMin", "mileage", "mileageMax", "mileage");
    private static final List<String> SEARCH_FIELDS = List.of("title", "model", "city", "brand.name", "seller.name");

    @Override
    @WithSession
    public Uni<PagedResult> query(ListQuery query) {
        Built built = PanacheListQuerySupport.build(query, FIELD_KINDS, SEARCH_FIELDS, RANGE_TARGET_COLUMNS);
        var panacheQuery = built.hasWhere()
                ? CarListing.<CarListing>find(built.where(), built.sort(), built.params())
                : CarListing.<CarListing>findAll(built.sort());
        panacheQuery.page(Page.of(query.from() / query.pageSize(), query.pageSize()));
        Uni<Long> count = built.hasWhere() ? CarListing.count(built.where(), built.params()) : CarListing.count();
        return Uni.combine().all().unis(count, panacheQuery.list())
                .asTuple()
                .map(t -> new PagedResult(t.getItem1(), t.getItem2().stream().map(this::toMap).toList()));
    }

    private List<String> readImages(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Corrupt images JSON in database", e);
        }
    }

    private Map<String, Object> toMap(CarListing listing) {
        Map<String, Object> map = mapper.convertValue(listing, new TypeReference<Map<String, Object>>() {
        });
        map.put("id", String.valueOf(listing.id));
        map.put("images", readImages(listing.getImages()));
        RelationIds.stringifyNestedIds(map);
        return map;
    }
}
