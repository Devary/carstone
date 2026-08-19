package org.devary.carstone.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.devary.table.annotations.CrudstoneEntity;
import org.devary.table.annotations.CrudstoneField;
import org.devary.table.annotations.FieldDependency;
import org.devary.table.annotations.Searchable;
import org.devary.table.annotations.SearchableField;
import org.devary.table.annotations.SearchResult;

import java.time.LocalDate;

/**
 * The star entity — an autoscout24-style car listing. Exercises the new
 * {@link SearchableField#rangeTarget()} mechanism three times (year/price/mileage), the first
 * real use of it anywhere in the ecosystem.
 *
 * <p>{@code yearFrom}/{@code yearTo}/{@code priceFrom}/{@code priceTo}/{@code mileageFrom}/
 * {@code mileageTo} are {@link Transient} — they have no backing column, they exist purely so
 * context-gen's reflection finds them and the search UI renders a "from"/"to" filter pair that
 * bounds the REAL {@code year}/{@code price}/{@code mileage} columns via
 * {@code rangeTarget}. They're excluded from the admin CRUD form entirely via
 * {@code disabledFields} below — there's nothing to save into a field with no column.
 *
 * <p>{@code year}/{@code price}/{@code mileage}/{@code power} all need
 * {@code createEditType = "inputText"} explicitly: neither {@code crudstone}'s admin form nor
 * {@code searchcrudstone}'s own numeric-only widgets exist yet for a bare {@code type="number"}
 * field (a confirmed gap, not a hypothetical) — the create/edit FORM falls back to a plain text
 * input via this override; the search-crudstone frontend fix (a real {@code isNumber} filter
 * control) is separate library work, not something declared here.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@CrudstoneEntity(
        name = "carListings",
        path = "listings",
        disabledFields = {"id", "yearFrom", "yearTo", "priceFrom", "priceTo", "mileageFrom", "mileageTo"},
        allowExportAction = true,
        showGlobalSearchBar = true,
        resizableColumns = true,
        theme = "blue"
)
@Searchable(theme = "blue", externalResult = true)
public class CarListing extends PanacheEntity {

    @CrudstoneField(type = "inputText", notNull = true)
    @SearchableField(external = true, order = 1, style = "col-md-3")
    @SearchResult(order = 1)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER)
    @CrudstoneField(type = "simpleSelect", listType = "brands", createEditType = "simpleSelect", promptView = true, notNull = true)
    @SearchableField(external = true, order = 2, style = "col-md-2", single = true)
    @SearchResult(order = 2)
    private Brand brand;

    @CrudstoneField(type = "inputText")
    @SearchableField(external = true, order = 3, style = "col-md-2")
    @SearchResult(order = 3)
    private String model;

    @CrudstoneField(type = "number", createEditType = "inputText", notNull = true)
    @SearchResult(order = 4)
    private Integer year;

    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(external = true, order = 4, style = "col-md-2", rangeTarget = "year", dependency = FieldDependency.GREATER_THAN_OR_EQUAL)
    private Integer yearFrom;

    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(external = true, order = 5, style = "col-md-2", rangeTarget = "year", dependency = FieldDependency.LESS_THAN_OR_EQUAL)
    private Integer yearTo;

    @CrudstoneField(type = "number", createEditType = "inputText", notNull = true)
    @SearchResult(order = 5)
    private Integer price;

    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(external = true, order = 6, style = "col-md-2", rangeTarget = "price", dependency = FieldDependency.GREATER_THAN_OR_EQUAL)
    private Integer priceFrom;

    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(external = true, order = 7, style = "col-md-2", rangeTarget = "price", dependency = FieldDependency.LESS_THAN_OR_EQUAL)
    private Integer priceTo;

    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchResult(order = 6)
    private Integer mileage;

    // mileage's own from/to pair lives in the "Filters" dropdown (external = false), not the main
    // bar — matches autoscout24's own layout, where brand/model/price/year are up-front and
    // mileage/fuel/transmission/body/color are secondary
    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(order = 1, rangeTarget = "mileage", dependency = FieldDependency.GREATER_THAN_OR_EQUAL)
    private Integer mileageFrom;

    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(order = 2, rangeTarget = "mileage", dependency = FieldDependency.LESS_THAN_OR_EQUAL)
    private Integer mileageTo;

    // hp — no range pair (kept out of scope; a plain result/table field, not itself filterable)
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchResult(order = 7)
    private Integer power;

    @CrudstoneField(type = "enum", enumClass = FuelType.class)
    @SearchableField(external = true, order = 8, style = "col-md-2")
    @SearchResult(order = 8)
    private String fuelType;

    @CrudstoneField(type = "enum", enumClass = Transmission.class)
    @SearchableField(order = 3)
    private String transmission;

    @CrudstoneField(type = "enum", enumClass = BodyType.class)
    @SearchableField(order = 4)
    private String bodyType;

    @CrudstoneField(type = "enum", enumClass = Color.class)
    @SearchableField(order = 5)
    private String color;

    // denormalized copy of seller.type, set at create/seed time — see class javadoc and
    // Seller's own javadoc for why this exists alongside the seller relation below
    @CrudstoneField(type = "enum", enumClass = SellerType.class, notNull = true)
    @SearchableField(external = true, order = 9, style = "col-md-2")
    @SearchResult(order = 9)
    private String sellerType;

    @ManyToOne(fetch = FetchType.EAGER)
    @CrudstoneField(type = "simpleSelect", listType = "sellers", createEditType = "simpleSelect", promptView = true, notNull = true)
    @SearchResult(order = 10)
    private Seller seller;

    @CrudstoneField(type = "inputText")
    @SearchableField(external = true, order = 10, style = "col-md-2")
    @SearchResult(order = 11)
    private String city;

    // gallery mode (multiple = true), base64 data URLs — same JSON-text-column convention as
    // Convention.images in quar-crud-host, (de)serialized by hand in CarListingHandler
    @Column(columnDefinition = "text")
    @CrudstoneField(type = "image", multiple = true, maxImages = 8, exportable = false)
    @SearchResult(order = 0)
    private String images;

    @Column(columnDefinition = "text")
    @CrudstoneField(type = "textEditor", showInTable = false)
    private String description;

    @CrudstoneField(type = "date", showInTable = false)
    private LocalDate firstRegistration;
}
