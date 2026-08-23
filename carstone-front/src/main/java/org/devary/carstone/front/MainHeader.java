package org.devary.carstone.front;

import org.devary.table.annotations.Header;
import org.devary.table.annotations.HeaderPosition;
import org.devary.table.annotations.HeaderVariant;
import org.devary.table.header.HeaderLink;
import org.devary.table.header.HeaderSection;

import java.util.List;

/**
 * carstone-front-ui's own site nav (previously a hand-coded {@code <nav>} in app.component.html
 * — see that repo's own git history) — same 3 links it already had, now declared generically and
 * served at {@code GET /header/main}. {@link HeaderVariant#SIMPLE_MENU} + a single, untitled
 * section: exactly the flat horizontal bar this site's own nav already was — see {@code @Header}'s
 * own javadoc for why a simple menu conventionally declares its links this way.
 */
@Header(name = "main", theme = "blue", variant = HeaderVariant.SIMPLE_MENU, position = HeaderPosition.LEFT)
public class MainHeader {

    private final List<HeaderSection> sections = List.of(
            HeaderSection.builder().links(List.of(
                    HeaderLink.link("Search", "/carListings"),
                    HeaderLink.link("About", "/about"),
                    HeaderLink.link("Contact", "/contact"))).build());
}
