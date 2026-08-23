package org.devary.carstone.front;

import org.devary.table.annotations.Header;
import org.devary.table.annotations.HeaderPosition;
import org.devary.table.annotations.HeaderVariant;
import org.devary.table.header.HeaderLink;
import org.devary.table.header.HeaderSection;

import java.util.List;

/**
 * carstone-front-ui's own site nav — a real {@link HeaderVariant#MEGA_MENU} demo, not an invented
 * one: every link's own {@code url} is a genuinely working destination, {@code /carListings?
 * <field>=<value>} — {@link HeaderComponent} (carstone-front-ui) recognizes that query-string
 * shape on click and pre-fills the search bar via the SAME router-navigation-state mechanism the
 * landing page's own "Browse by body type" cards already use (see that component's own history),
 * rather than a plain page load with no filter applied. Every value below is a real
 * {@link org.devary.carstone.domain.CarListing} enum constant (BodyType/FuelType/Color/
 * Transmission/SellerType) — deliberately NOT a "Browse by Brand" section: Brand is a relation
 * (a numeric database id, not a stable string), so a hardcoded id here would silently break the
 * moment seed data changes order, unlike a plain enum constant name.
 *
 * <p>5 sections, 31 links total — Body Type (8), Fuel Type (6), Color (10), Buying Options (4:
 * Transmission x2 + SellerType x2), Company (3: the plain, non-filtered real pages).
 */
@Header(name = "main", theme = "blue", variant = HeaderVariant.MEGA_MENU, position = HeaderPosition.LEFT)
public class MainHeader {

    private final List<HeaderSection> sections = List.of(
            HeaderSection.builder().title("Body Type").links(List.of(
                    HeaderLink.link("Sedan", "/carListings?bodyType=SEDAN"),
                    HeaderLink.link("SUV", "/carListings?bodyType=SUV"),
                    HeaderLink.link("Hatchback", "/carListings?bodyType=HATCHBACK"),
                    HeaderLink.link("Estate", "/carListings?bodyType=ESTATE"),
                    HeaderLink.link("Coupe", "/carListings?bodyType=COUPE"),
                    HeaderLink.link("Convertible", "/carListings?bodyType=CONVERTIBLE"),
                    HeaderLink.link("Van", "/carListings?bodyType=VAN"),
                    HeaderLink.link("Pickup", "/carListings?bodyType=PICKUP"))).build(),

            HeaderSection.builder().title("Fuel Type").links(List.of(
                    HeaderLink.link("Petrol", "/carListings?fuelType=PETROL"),
                    HeaderLink.link("Diesel", "/carListings?fuelType=DIESEL"),
                    HeaderLink.link("Electric", "/carListings?fuelType=ELECTRIC"),
                    HeaderLink.link("Hybrid", "/carListings?fuelType=HYBRID"),
                    HeaderLink.link("LPG", "/carListings?fuelType=LPG"),
                    HeaderLink.link("Other", "/carListings?fuelType=OTHER"))).build(),

            HeaderSection.builder().title("Color").links(List.of(
                    HeaderLink.link("Black", "/carListings?color=BLACK"),
                    HeaderLink.link("White", "/carListings?color=WHITE"),
                    HeaderLink.link("Grey", "/carListings?color=GREY"),
                    HeaderLink.link("Silver", "/carListings?color=SILVER"),
                    HeaderLink.link("Blue", "/carListings?color=BLUE"),
                    HeaderLink.link("Red", "/carListings?color=RED"),
                    HeaderLink.link("Green", "/carListings?color=GREEN"),
                    HeaderLink.link("Brown", "/carListings?color=BROWN"),
                    HeaderLink.link("Yellow", "/carListings?color=YELLOW"),
                    HeaderLink.link("Orange", "/carListings?color=ORANGE"))).build(),

            HeaderSection.builder().title("Buying Options").links(List.of(
                    HeaderLink.link("Manual", "/carListings?transmission=MANUAL"),
                    HeaderLink.link("Automatic", "/carListings?transmission=AUTOMATIC"),
                    HeaderLink.link("Dealerships", "/carListings?sellerType=PROFESSIONAL"),
                    HeaderLink.link("Private Sellers", "/carListings?sellerType=PRIVATE"))).build(),

            HeaderSection.builder().title("Company").links(List.of(
                    HeaderLink.link("Search all listings", "/carListings"),
                    HeaderLink.link("About us", "/about"),
                    HeaderLink.link("Contact us", "/contact"))).build());
}
