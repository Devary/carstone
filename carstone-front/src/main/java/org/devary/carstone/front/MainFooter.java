package org.devary.carstone.front;

import org.devary.table.annotations.Footer;
import org.devary.table.footer.FooterButton;
import org.devary.table.footer.FooterInput;
import org.devary.table.footer.FooterLink;
import org.devary.table.footer.FooterSection;

import java.util.List;

/**
 * carstone-front-ui's own site footer — previously a hardcoded {@code FooterConfig} object in
 * {@code app.component.ts} (see that repo's own git history), now served generically via
 * {@code GET /footer/main} the same way {@code @Sidebar} already backs carstone-admin's nav.
 */
@Footer(name = "main", theme = "blue", title = "Carstone")
public class MainFooter {

    // the frontend still appends "&copy; {current year}" live (never baked in here, so it can't
    // go stale) — this is just the brand name preceding it, kept as its own field so the pattern
    // stays consistent with "copyright text can contain links, rendered as HTML" even though this
    // particular value has none
    private final String copyrightHtml = "Carstone";

    private final List<FooterSection> sections = List.of(
            FooterSection.builder()
                    .style("col-md-4")
                    .description("The marketplace for buying and selling cars — private sellers and dealerships alike.")
                    .links(List.of(
                            FooterLink.builder().label("Facebook").url("https://facebook.com").logo("pi pi-facebook").build(),
                            FooterLink.builder().label("Instagram").url("https://instagram.com").logo("pi pi-instagram").build(),
                            FooterLink.builder().label("X").url("https://x.com").logo("pi pi-twitter").build()))
                    .build(),
            FooterSection.builder()
                    .style("col-md-4")
                    .title("Company")
                    .links(List.of(
                            FooterLink.builder().label("About us").url("/about").build(),
                            FooterLink.builder().label("Contact us").url("/contact").build()))
                    .build(),
            FooterSection.builder()
                    .style("col-md-4")
                    .title("Browse")
                    .links(List.of(
                            FooterLink.builder().label("Search listings").url("/carListings").build()))
                    .build(),
            FooterSection.builder()
                    .style("col-md-4")
                    .title("Get in touch")
                    .input(FooterInput.builder().placeholder("Your email").type("email").name("email").build())
                    .buttons(List.of(FooterButton.builder().label("Email us").href("mailto:hello@carstone.dev").build()))
                    .build());
}
