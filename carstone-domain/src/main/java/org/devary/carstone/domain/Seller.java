package org.devary.carstone.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.devary.table.annotations.CrudstoneEntity;
import org.devary.table.annotations.CrudstoneField;
import org.devary.table.annotations.LinkType;

/**
 * A dealership ({@link SellerType#PROFESSIONAL}) or an individual owner
 * ({@link SellerType#PRIVATE}) posting {@link CarListing}s. Full CRUD-manageable in the admin;
 * referenced by {@link CarListing#seller} for display (promptView) — filtering by seller TYPE
 * happens via {@link CarListing#sellerType}'s own denormalized copy, not a drill-down into this
 * entity, since "dealer vs private" (not "which specific seller") is the real autoscout24 filter.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@CrudstoneEntity(name = "sellers", path = "sellers", disabledFields = "id")
public class Seller extends PanacheEntity {

    @CrudstoneField(type = "inputText", notNull = true)
    private String name;

    @CrudstoneField(type = "enum", enumClass = SellerType.class, notNull = true)
    private String type;

    @CrudstoneField(type = "inputText")
    private String city;

    @CrudstoneField(type = "inputText", linkType = LinkType.PHONE)
    private String phone;

    @CrudstoneField(type = "inputText", linkType = LinkType.EMAIL)
    private String email;
}
