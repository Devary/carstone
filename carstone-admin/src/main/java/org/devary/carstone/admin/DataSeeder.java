package org.devary.carstone.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.devary.carstone.domain.Brand;
import org.devary.carstone.domain.BodyType;
import org.devary.carstone.domain.CarListing;
import org.devary.carstone.domain.Color;
import org.devary.carstone.domain.FuelType;
import org.devary.carstone.domain.Seller;
import org.devary.carstone.domain.SellerType;
import org.devary.carstone.domain.Transmission;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Seeds ~20 real car brands, ~20 sellers (mixed professional/private), and ~90 car listings —
 * modeled on quar-crud-host's own {@code DataSeeder}: same Vert.x duplicated-context boilerplate
 * (Hibernate Reactive needs one to attach a session to, and there isn't one yet this early in
 * startup), same deterministic array-cycling-by-loop-index generation (no faker dependency).
 */
@ApplicationScoped
public class DataSeeder {

    @Inject
    Vertx vertx;

    @Inject
    ObjectMapper mapper;

    private static final String[][] BRANDS = {
            {"BMW", "Germany"}, {"Mercedes-Benz", "Germany"}, {"Audi", "Germany"},
            {"Volkswagen", "Germany"}, {"Porsche", "Germany"}, {"Opel", "Germany"},
            {"Toyota", "Japan"}, {"Honda", "Japan"}, {"Nissan", "Japan"}, {"Mazda", "Japan"},
            {"Hyundai", "South Korea"}, {"Kia", "South Korea"},
            {"Ford", "USA"}, {"Tesla", "USA"},
            {"Renault", "France"}, {"Peugeot", "France"}, {"Citroen", "France"},
            {"Fiat", "Italy"}, {"Volvo", "Sweden"}, {"Skoda", "Czech Republic"}, {"SEAT", "Spain"},
    };

    private static final String[] DEALERSHIP_NAMES = {
            "AutoWorld Dealership", "Premium Motors", "CityCars Trading", "Elite Auto Sales",
            "Sunrise Automotive", "Metro Car Center", "Highway Motors", "Prestige Vehicles",
    };
    private static final String[] PRIVATE_SELLER_NAMES = {
            "Jean Dupont", "Anna Schmidt", "Marco Rossi", "Sophie Martin", "Lukas Becker",
            "Maria Garcia", "Tom Janssen", "Nina Kowalski", "Erik Andersson", "Julia Novak",
            "Paul Bernard", "Laura Fischer",
    };
    private static final String[] CITIES = {
            "Paris", "Berlin", "Munich", "Milan", "Madrid", "Amsterdam", "Brussels", "Vienna",
            "Zurich", "Lyon", "Hamburg", "Rome", "Barcelona", "Rotterdam", "Frankfurt",
    };
    private static final String[] MODEL_NAMES = {
            "Series 3", "C-Class", "A4", "Golf", "911", "Corsa", "Corolla", "Civic", "Qashqai",
            "CX-5", "Tucson", "Sportage", "Focus", "Model 3", "Clio", "3008", "C4", "500 X",
            "XC60", "Octavia", "Leon",
    };

    void seed(@Observes StartupEvent event) {
        // Hibernate Reactive needs a Vert.x *duplicated* context (Quarkus' per-request isolation
        // mechanism) to attach its session to — there isn't one yet this early in startup, so one
        // is created and explicitly marked safe by hand, same as quar-crud-host's own DataSeeder.
        // This thread blocks on a CompletableFuture until the reactive pipeline finishes, so seed
        // data is guaranteed ready before the app finishes booting.
        CompletableFuture<Void> seeded = new CompletableFuture<>();
        io.vertx.core.Context duplicated = VertxContext.getOrCreateDuplicatedContext(vertx.getDelegate());
        io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe(duplicated, true);
        io.vertx.mutiny.core.Context.newInstance(duplicated).runOnContext(() -> seedPostgres().subscribe().with(
                ignored -> seeded.complete(null),
                seeded::completeExceptionally));
        seeded.join();
    }

    private Uni<Void> seedPostgres() {
        return Panache.withTransaction(() -> {
            List<Brand> brands = buildBrands();
            List<Seller> sellers = buildSellers();
            return Brand.persist(brands)
                    .chain(() -> Seller.persist(sellers))
                    .chain(() -> CarListing.persist(buildListings(brands, sellers)))
                    .replaceWithVoid();
        });
    }

    private List<Brand> buildBrands() {
        List<Brand> brands = new ArrayList<>();
        for (String[] nameCountry : BRANDS) {
            Brand brand = new Brand();
            brand.setName(nameCountry[0]);
            brand.setCountry(nameCountry[1]);
            brands.add(brand);
        }
        return brands;
    }

    private List<Seller> buildSellers() {
        List<Seller> sellers = new ArrayList<>();
        for (int i = 0; i < DEALERSHIP_NAMES.length; i++) {
            Seller seller = new Seller();
            seller.setName(DEALERSHIP_NAMES[i]);
            seller.setType(SellerType.PROFESSIONAL.name());
            seller.setCity(CITIES[i % CITIES.length]);
            String slug = DEALERSHIP_NAMES[i].toLowerCase().replaceAll("[^a-z0-9]+", "");
            seller.setPhone("+33 1 " + String.format("%02d", 40 + i) + " 00 00 " + String.format("%02d", i));
            seller.setEmail("contact@" + slug + ".example.com");
            sellers.add(seller);
        }
        for (int i = 0; i < PRIVATE_SELLER_NAMES.length; i++) {
            Seller seller = new Seller();
            seller.setName(PRIVATE_SELLER_NAMES[i]);
            seller.setType(SellerType.PRIVATE.name());
            seller.setCity(CITIES[(i + 5) % CITIES.length]);
            String slug = PRIVATE_SELLER_NAMES[i].toLowerCase().replaceAll("[^a-z0-9]+", ".");
            seller.setPhone("+33 6 " + String.format("%02d", 10 + i) + " 00 00 " + String.format("%02d", i));
            seller.setEmail(slug + "@example.com");
            sellers.add(seller);
        }
        return sellers;
    }

    private List<CarListing> buildListings(List<Brand> brands, List<Seller> sellers) {
        List<CarListing> listings = new ArrayList<>();
        FuelType[] fuelTypes = FuelType.values();
        Transmission[] transmissions = Transmission.values();
        BodyType[] bodyTypes = BodyType.values();
        Color[] colors = Color.values();

        for (int i = 0; i < 90; i++) {
            Brand brand = brands.get(i % brands.size());
            Seller seller = sellers.get(i % sellers.size());
            String model = MODEL_NAMES[i % MODEL_NAMES.length];
            int year = 2015 + (i % 10);
            int mileage = 5000 + (i * 733) % 150_000;
            int price = 9000 + (i * 977) % 65_000;
            int power = 90 + (i * 13) % 300;

            CarListing listing = new CarListing();
            listing.setTitle(brand.getName() + " " + model);
            listing.setBrand(brand);
            listing.setModel(model);
            listing.setYear(year);
            listing.setMileage(mileage);
            listing.setPrice(price);
            listing.setPower(power);
            listing.setFuelType(fuelTypes[i % fuelTypes.length].name());
            listing.setTransmission(transmissions[i % transmissions.length].name());
            listing.setBodyType(bodyTypes[i % bodyTypes.length].name());
            listing.setColor(colors[i % colors.length].name());
            listing.setSellerType(seller.getType());
            listing.setSeller(seller);
            listing.setCity(CITIES[i % CITIES.length]);
            listing.setImages(toJson(List.of()));
            listing.setDescription(year + " " + brand.getName() + " " + model + ", " + mileage
                    + " km, " + fuelTypes[i % fuelTypes.length].name().toLowerCase() + ", listed by "
                    + seller.getName() + " in " + listing.getCity() + ".");
            listing.setFirstRegistration(LocalDate.of(year, 1 + (i % 12), 1 + (i % 27)));

            listings.add(listing);
        }
        return listings;
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize seed images", e);
        }
    }
}
