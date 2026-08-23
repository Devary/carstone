package org.devary.carstone.front;

import org.devary.table.annotations.ContactForm;
import org.devary.table.contact.ContactFormField;

import java.util.List;

/**
 * carstone-front-ui's own "Get in touch" contact page (previously a hardcoded form with no real
 * submission target — see that repo's own git history) — same 3 fields it already had, now
 * declared generically and submitted through {@code POST /contact-form/main}, handled by
 * {@link MainContactFormHandler}.
 */
@ContactForm(name = "main", title = "Get in touch",
        description = "Questions about a listing, selling your car, or a dealership partnership? Send us a message.")
public class MainContactForm {

    private final List<ContactFormField> fields = List.of(
            ContactFormField.builder().name("name").label("Name").type("text").required(true).build(),
            ContactFormField.builder().name("email").label("Email").type("email").required(true).build(),
            ContactFormField.builder().name("message").label("Message").type("textarea").required(true).build());
}
