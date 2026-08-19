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
import org.devary.carstone.domain.CarListing;
import org.devary.carstone.domain.Seller;
import org.devary.carstone.domain.support.PanacheListQuerySupport;
import org.devary.carstone.domain.support.PanacheListQuerySupport.Built;
import org.devary.carstone.domain.support.PanacheListQuerySupport.FieldKind;
import org.devary.carstone.domain.support.RelationIds;
import org.devary.table.annotations.CrudstoneResource;
import org.devary.table.crud.CrudHandler;
import org.devary.table.crud.ListQuery;
import org.devary.table.crud.PagedResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persists CarListing via Hibernate Reactive Panache — modeled directly on quar-crud-host's
 * {@code ConventionHandler}. Three fields need special handling: {@code brand}/{@code seller}
 * (real {@code @ManyToOne}s — the incoming {id, ...} object is resolved down to just its id,
 * then looked up as a managed reference), and {@code images} (JSON text in the database vs. an
 * array on the wire, same reasoning as {@code Convention.images}). {@code yearFrom}/
 * {@code yearTo}/{@code priceFrom}/{@code priceTo}/{@code mileageFrom}/{@code mileageTo} never
 * reach this class at all — they're excluded from the admin context via
 * {@code CrudstoneEntity#disabledFields}, so the admin form never renders or sends them.
 */
@ApplicationScoped
@CrudstoneResource(entity = CarListing.class)
public class CarListingHandler implements CrudHandler {

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
    @WithTransaction
    public Uni<Map<String, Object>> create(Map<String, Object> body) {
        CarListing listing = new CarListing();
        return apply(listing, body)
                .chain(() -> listing.<CarListing>persist())
                .map(this::toMap);
    }

    @Override
    @WithTransaction
    public Uni<Optional<Map<String, Object>>> update(String id, Map<String, Object> body) {
        return CarListing.<CarListing>findById(Long.valueOf(id)).chain(listing -> {
            if (listing == null) {
                return Uni.createFrom().item(Optional.<Map<String, Object>>empty());
            }
            return apply(listing, body).map(ignored -> Optional.of(toMap(listing)));
        });
    }

    @Override
    @WithTransaction
    public Uni<Boolean> delete(String id) {
        return CarListing.deleteById(Long.valueOf(id));
    }

    // real database-pushed pagination/sort/filter/search, RANGE_MIN/MAX entries are the backend
    // half of context-gen's new SearchableField#rangeTarget mechanism — wire field names
    // "yearMin"/"yearMax"/etc (what search-crudstone's wire-filters.ts submits for a rangeTarget
    // field, per its own dependency: GTE -> "Min", LTE -> "Max") bound the REAL year/price/mileage
    // columns named in RANGE_TARGET_COLUMNS, not columns of their own.
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

    private Uni<Void> apply(CarListing listing, Map<String, Object> body) {
        Map<String, Object> scalarFields = new LinkedHashMap<>(body);
        scalarFields.remove("brand");
        scalarFields.remove("seller");
        scalarFields.put("images", writeJson(body.get("images")));

        // frontend always sends a full ISO datetime even for a date-only picker; LocalDate.parse
        // (which Jackson uses under the hood) only accepts the plain "yyyy-MM-dd" form — same
        // fix ConventionHandler applies to its own LocalDate fields
        Object firstRegistration = body.get("firstRegistration");
        if (firstRegistration != null) {
            scalarFields.put("firstRegistration", firstRegistration.toString().substring(0, 10));
        }

        try {
            mapper.updateValue(listing, scalarFields);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid car listing payload", e);
        }

        Uni<Void> brand;
        Object brandValue = body.get("brand");
        if (brandValue instanceof Map<?, ?> map && map.get("id") != null) {
            brand = Brand.<Brand>findById(Long.valueOf(map.get("id").toString()))
                    .invoke(listing::setBrand)
                    .replaceWithVoid();
        } else {
            listing.setBrand(null);
            brand = Uni.createFrom().voidItem();
        }

        // chained, not combined: a Hibernate Reactive session can't run two queries
        // concurrently within the same transaction (Uni.combine().all() on two findById()
        // calls here throws "Illegal pop() with non-matching JdbcValuesSourceProcessingState",
        // confirmed live) — brand must fully resolve before seller's own query starts
        return brand.chain(() -> {
            Object sellerValue = body.get("seller");
            if (sellerValue instanceof Map<?, ?> map && map.get("id") != null) {
                return Seller.<Seller>findById(Long.valueOf(map.get("id").toString()))
                        // sellerType is denormalized from the linked seller's own type at
                        // write time — see CarListing's own javadoc for why
                        .invoke(found -> {
                            listing.setSeller(found);
                            listing.setSellerType(found.getType());
                        })
                        .replaceWithVoid();
            }
            listing.setSeller(null);
            return Uni.createFrom().voidItem();
        });
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON field value", e);
        }
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
