package org.devary.carstone.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.devary.table.annotations.CrudstoneEntity;
import org.devary.table.annotations.CrudstoneField;

/**
 * A car manufacturer (BMW, Toyota, ...) — a pure lookup/relation target referenced by
 * {@link CarListing#brand}. Full CRUD-manageable in the admin (carstone-admin), never itself
 * {@link org.devary.table.annotations.Searchable} — nobody searches "for a brand", they search
 * for listings and filter BY brand via CarListing's own relation field.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@CrudstoneEntity(name = "brands", path = "brands", disabledFields = "id")
public class Brand extends PanacheEntity {

    @CrudstoneField(type = "inputText", notNull = true)
    private String name;

    @CrudstoneField(type = "inputText")
    private String country;

    // base64 data URL from the frontend's file-upload widget, same convention as
    // Convention.image in quar-crud-host — left null/empty for seeded brands rather than
    // investing in fake binary logo assets
    @CrudstoneField(type = "image", exportable = false)
    private String logo;
}
