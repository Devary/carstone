package org.devary.carstone.gateway;

import org.devary.carstone.gateway.registry.RegisteredService;
import org.devary.carstone.gateway.registry.ServiceRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Ported from quar-gateway's own {@code GatewayProxyService} — same plain
 * {@code java.net.http.HttpClient} forward, same header-stripping rules. The ONE real
 * difference: {@code gatewayAuthorizationService.authorize(...)} (Keycloak/Vault/Redzone
 * token-verification) is gone entirely — this gateway has zero auth, every request is forwarded
 * unconditionally to whichever backend {@code serviceName} names.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GatewayProxyService {

    private static final Logger LOG = Logger.getLogger(GatewayProxyService.class);

    private static final List<String> REQUEST_HEADERS_TO_SKIP = List.of("host", "content-length", "transfer-encoding", "connection");
    private static final List<String> RESPONSE_HEADERS_TO_SKIP = List.of("content-length", "transfer-encoding", "connection");

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final ServiceRegistry serviceRegistry;
    // HTTP_1_1 pinned deliberately: the JDK HttpClient defaults to preferring HTTP/2 and
    // opportunistically negotiating it per-connection — against Quarkus dev-mode's HTTP
    // endpoint (carstone-admin/carstone-front here), the first request(s) after a fresh boot
    // reproducibly fail with "java.io.IOException: Received RST_STREAM: Protocol error" before
    // the client falls back, confirmed live (every real browser page load hit this — two
    // near-simultaneous requests firing right after a restart — while spaced-out manual curl
    // calls almost never did, since by then a working connection was already established).
    // Both backends are plain cleartext HTTP/1.1 services; there's no reason to negotiate HTTP/2
    // with them at all.
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public Response proxy(String method,
                           String serviceName,
                           String downstreamPath,
                           byte[] body,
                           HttpHeaders headers,
                           UriInfo uriInfo) {
        RegisteredService service = serviceRegistry.findService(serviceName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown service: " + serviceName));

        URI targetUri = buildTargetUri(service.baseUrl(), downstreamPath, uriInfo);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(targetUri)
                .timeout(REQUEST_TIMEOUT)
                .method(method, bodyPublisher(method, body));

        copyRequestHeaders(headers, requestBuilder);

        long startNanos = System.nanoTime();
        LOG.infof("Gateway proxy start method=%s service=%s targetUri=%s bodyBytes=%d",
                method, serviceName, targetUri, body == null ? 0 : body.length);

        try {
            HttpResponse<byte[]> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            LOG.infof("Gateway proxy finish method=%s service=%s status=%d elapsedMs=%d",
                    method, serviceName, response.statusCode(), elapsedMs);
            return toJaxRsResponse(response);
        } catch (IOException e) {
            LOG.errorf(e, "Gateway proxy io-failure method=%s service=%s targetUri=%s", method, serviceName, targetUri);
            throw new WebApplicationException("Downstream service is unreachable: " + serviceName, e, Response.Status.BAD_GATEWAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.errorf(e, "Gateway proxy interrupted method=%s service=%s targetUri=%s", method, serviceName, targetUri);
            throw new WebApplicationException("Gateway request interrupted", e, Response.Status.BAD_GATEWAY);
        }
    }

    private URI buildTargetUri(String baseUrl, String downstreamPath, UriInfo uriInfo) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = (downstreamPath == null || downstreamPath.isBlank()) ? "" : "/" + stripLeadingSlash(downstreamPath);
        String query = uriInfo.getRequestUri().getRawQuery();
        return URI.create(normalizedBaseUrl + normalizedPath + (query == null || query.isBlank() ? "" : "?" + query));
    }

    private HttpRequest.BodyPublisher bodyPublisher(String method, byte[] body) {
        if (body == null || body.length == 0) {
            return requiresRequestBody(method)
                    ? HttpRequest.BodyPublishers.ofByteArray(new byte[0])
                    : HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private boolean requiresRequestBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private void copyRequestHeaders(HttpHeaders headers, HttpRequest.Builder requestBuilder) {
        for (Map.Entry<String, List<String>> entry : headers.getRequestHeaders().entrySet()) {
            if (REQUEST_HEADERS_TO_SKIP.contains(entry.getKey().toLowerCase())) {
                continue;
            }
            for (String value : entry.getValue()) {
                requestBuilder.header(entry.getKey(), value);
            }
        }
    }

    private Response toJaxRsResponse(HttpResponse<byte[]> response) {
        Response.ResponseBuilder builder = Response.status(response.statusCode()).entity(response.body());
        response.headers().map().forEach((name, values) -> {
            if (shouldSkipResponseHeader(name)) {
                return;
            }
            values.forEach(value -> builder.header(name, value));
        });
        return builder.build();
    }

    private boolean shouldSkipResponseHeader(String headerName) {
        String normalized = headerName.toLowerCase();
        return RESPONSE_HEADERS_TO_SKIP.contains(normalized) || normalized.startsWith(":");
    }

    private String stripLeadingSlash(String value) {
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
