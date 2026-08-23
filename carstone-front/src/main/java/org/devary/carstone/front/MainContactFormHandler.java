package org.devary.carstone.front;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.devary.table.annotations.ContactFormResource;
import org.devary.table.contact.ContactFormHandler;

import java.util.Map;

/**
 * The customization point {@link org.devary.table.contact.ContactFormHandler}'s own javadoc
 * describes — this is where a real deployment plugs in whatever it actually wants done with a
 * submission. Here: forward it as an email via Quarkus' own {@code quarkus-mailer} extension (a
 * real, working example, not a stub — dev/test mode mocks the actual SMTP send, see this
 * project's {@code application.properties}). A different app might instead call a third-party
 * mailing/CRM API's HTTP client, post to a Slack webhook, or write a database row — this
 * interface doesn't care which; only {@code submit()}'s own body would change.
 */
@ApplicationScoped
@ContactFormResource(form = MainContactForm.class)
public class MainContactFormHandler implements ContactFormHandler {

    @Inject
    ReactiveMailer mailer;

    @Override
    public Uni<Void> submit(Map<String, Object> body) {
        String name = String.valueOf(body.get("name"));
        String email = String.valueOf(body.get("email"));
        String message = String.valueOf(body.get("message"));

        return mailer.send(Mail.withText(
                "sales@carstone.dev",
                "New contact form message from " + name,
                "From: " + name + " <" + email + ">\n\n" + message));
    }
}
