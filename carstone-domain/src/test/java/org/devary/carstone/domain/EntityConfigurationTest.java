package org.devary.carstone.domain;

import org.devary.table.TableContext;
import org.devary.table.search.SearchContext;
import org.devary.table.search.SearchField;
import org.devary.table.utils.AnnotationContextLoader;
import org.devary.table.utils.AnnotationSearchContextLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Earliest possible checkpoint that the annotation config on Brand/Seller/CarListing (in
 * particular the new SearchableField#rangeTarget usages) is actually valid — catches a
 * range-config mistake here rather than waiting for a full Quarkus boot in carstone-admin.
 */
class EntityConfigurationTest {

    private final AnnotationContextLoader contextLoader = new AnnotationContextLoader();
    private final AnnotationSearchContextLoader searchLoader = new AnnotationSearchContextLoader();

    @Test
    void brandAndSellerLoadAsPlainCrudContexts() {
        TableContext brand = contextLoader.load(Brand.class);
        assertThat(brand.getName()).isEqualTo("brands");

        TableContext seller = contextLoader.load(Seller.class);
        assertThat(seller.getName()).isEqualTo("sellers");
    }

    @Test
    void carListingLoadsAsBothACrudContextAndASearchContext() {
        TableContext table = contextLoader.load(CarListing.class);
        assertThat(table.getName()).isEqualTo("carListings");
        assertThat(table.getDisabledFields()).contains(
                "id", "yearFrom", "yearTo", "priceFrom", "priceTo", "mileageFrom", "mileageTo");

        SearchContext search = searchLoader.load(CarListing.class);
        assertThat(search.getExternalResult()).isTrue();
    }

    @Test
    void rangeFieldsResolveAgainstTheirRealBackingColumns() {
        SearchContext search = searchLoader.load(CarListing.class);

        assertRangeField(search, "yearFrom", "year", "GREATER_THAN_OR_EQUAL");
        assertRangeField(search, "yearTo", "year", "LESS_THAN_OR_EQUAL");
        assertRangeField(search, "priceFrom", "price", "GREATER_THAN_OR_EQUAL");
        assertRangeField(search, "priceTo", "price", "LESS_THAN_OR_EQUAL");
        assertRangeField(search, "mileageFrom", "mileage", "GREATER_THAN_OR_EQUAL");
        assertRangeField(search, "mileageTo", "mileage", "LESS_THAN_OR_EQUAL");
    }

    private void assertRangeField(SearchContext search, String fieldName, String expectedTarget, String expectedDependency) {
        SearchField field = search.getFields().stream()
                .filter(f -> f.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No field named '" + fieldName + "'"));
        assertThat(field.getRangeTarget()).isEqualTo(expectedTarget);
        assertThat(field.getDependency()).isEqualTo(expectedDependency);
    }
}
