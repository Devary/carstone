package org.devary.carstone.domain;

/**
 * A seller of a car listing — a real dealership ("PROFESSIONAL") or an individual owner
 * ("PRIVATE"). {@link Seller#type} is the source of truth; {@link CarListing#sellerType} is a
 * denormalized copy of the linked seller's own value, set at create/seed time, purely so
 * top-level "Dealer vs Private seller" filtering on a listing doesn't require a relation
 * drill-down through the facet-tree mechanism.
 */
public enum SellerType {
    PROFESSIONAL,
    PRIVATE,
}
