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
        title = "Car Listings",
        disabledFields = {"id", "yearFrom", "yearTo", "priceFrom", "priceTo", "mileageFrom", "mileageTo"},
        allowExportAction = true,
        showGlobalSearchBar = true,
        resizableColumns = true,
        theme = "blue"
)
@Searchable(theme = "blue", externalResult = true)
public class CarListing extends PanacheEntity {

    // NOT external: with 10 external fields the main bar was too cramped to read any
    // placeholder (user-reported) — only the 4 "major" concepts a real autoscout24-style search
    // actually leads with (Brand, Model, Price range, Year range = 6 controls, exactly one
    // 12-col row) stay always-visible; everything else moved into the "Filters" dropdown below.
    @CrudstoneField(type = "inputText", notNull = true)
    @SearchableField(order = 6)
    @SearchResult(order = 1)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER)
    @CrudstoneField(type = "simpleSelect", listType = "brands", createEditType = "simpleSelect", promptView = true, notNull = true)
    @SearchableField(external = true, order = 1, style = "col-md-2", single = true)
    @SearchResult(order = 2)
    private Brand brand;

    @CrudstoneField(type = "inputText")
    @SearchableField(external = true, order = 2, style = "col-md-2")
    @SearchResult(order = 3)
    private String model;

    // minValue=1950: a real car's first-registration year never predates the automobile
    // industry's own modern era — the field used to accept anything from 0, which is what
    // motivated adding CrudstoneField#minValue/#maxValue in the first place (user-reported).
    // No range=true here: year stays two separate year pickers, not a slider (see price below
    // for the slider treatment) — a specific year is more naturally picked than dragged.
    // noFutureValue=true: a listing can't claim a first-registration year later than the current
    // one (user-reported) — the effective cap becomes min(maxValue, current year) every year,
    // never a fixed value baked in here; maxValue=2030 is left as harmless future headroom rather
    // than trimmed, since noFutureValue is what actually enforces the real-world constraint now.
    // yearPicker=true: the search filter renders as a year-only date picker instead of a plain
    // number input (user-reported) — out-of-range years are disabled directly in the picker,
    // rather than relying on a blur-time clamp the way the plain-input case still needs to.
    @CrudstoneField(type = "number", createEditType = "inputText", notNull = true, minValue = 1950, maxValue = 2030, noFutureValue = true, yearPicker = true)
    @SearchResult(order = 4)
    private Integer year;

    // style="col-md-3" (up from col-md-2, user-reported the pair looked cramped/truncated once
    // it became a date-picker button+icon instead of a plain number input — a narrower column
    // that read fine as "Yea|" of bare text left no room once an icon competed for the same
    // space, both fields collapsing to the same unreadable "Ye" placeholder with no visual way
    // to tell From/To apart). priceFrom below gives back the column this borrows.
    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(external = true, order = 5, style = "col-md-3", rangeTarget = "year", dependency = FieldDependency.GREATER_THAN_OR_EQUAL)
    private Integer yearFrom;

    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(external = true, order = 6, style = "col-md-3", rangeTarget = "year", dependency = FieldDependency.LESS_THAN_OR_EQUAL)
    private Integer yearTo;

    // minValue=500/maxValue=500_000: a real-world used-to-exotic car price range (also
    // user-reported: the field previously accepted anything down to 0). range=true: the
    // priceFrom/priceTo pair below renders as ONE PrimeNG dual-handle slider spanning this
    // bound instead of two separate number inputs (search-crudstone's own new
    // isRangeSliderFrom/isRangeSliderTo mechanism).
    @CrudstoneField(type = "number", createEditType = "inputText", notNull = true, minValue = 500, maxValue = 500_000, range = true)
    @SearchResult(order = 5)
    private Integer price;

    // style="col-md-3" (down from col-md-4, giving that column back to yearFrom/yearTo above,
    // which needed it more — a slider's own label already reads compactly as "Price From: 500 –
    // 500000" regardless of the track's pixel width, unlike a picker button competing with an
    // icon for a readable placeholder): priceFrom is the ONLY rendered control for the pair
    // (priceTo renders nothing of its own, see search-crudstone's isRangeSliderTo).
    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(external = true, order = 3, style = "col-md-3", rangeTarget = "price", dependency = FieldDependency.GREATER_THAN_OR_EQUAL)
    private Integer priceFrom;

    @Transient
    @CrudstoneField(type = "number", createEditType = "inputText")
    @SearchableField(external = true, order = 4, style = "col-md-2", rangeTarget = "price", dependency = FieldDependency.LESS_THAN_OR_EQUAL)
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
    @SearchableField(order = 7)
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
    // Seller's own javadoc for why this exists alongside the seller relation below. NOT
    // notNull: the client never submits this directly (CarListingHandler derives it from the
    // required `seller` relation after the generic wire-body validator would already run), so a
    // client-side "must be present in the request" constraint would reject every legitimate
    // create/update before the handler ever gets a chance to fill it in.
    @CrudstoneField(type = "enum", enumClass = SellerType.class)
    @SearchableField(order = 8)
    @SearchResult(order = 9)
    private String sellerType;

    @ManyToOne(fetch = FetchType.EAGER)
    @CrudstoneField(type = "simpleSelect", listType = "sellers", createEditType = "simpleSelect", promptView = true, notNull = true)
    @SearchResult(order = 10)
    private Seller seller;

    @CrudstoneField(type = "inputText")
    @SearchableField(order = 9)
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
