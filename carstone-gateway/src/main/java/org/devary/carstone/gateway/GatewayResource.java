package org.devary.carstone.gateway;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;

/**
 * Ported from quar-gateway's own {@code GatewayResource} — same {@code /{service}/{path: .*}}
 * wildcard routing for every HTTP verb. The route-introspection endpoints ({@code GET /routes},
 * {@code GET /{service}}) and every OpenAPI annotation were dropped (optional read-only sugar,
 * not needed for a 2-backend demo gateway).
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GatewayResource {

    private static final Logger LOG = Logger.getLogger(GatewayResource.class);

    private final GatewayProxyService gatewayProxyService;

    @GET
    @Path("/{service}/{path: .*}")
    @Consumes(MediaType.WILDCARD)
    public Response get(@PathParam("service") String service,
                         @PathParam("path") String path,
                         @Context HttpHeaders headers,
                         @Context UriInfo uriInfo) {
        return forward("GET", service, path, null, headers, uriInfo);
    }

    @POST
    @Path("/{service}/{path: .*}")
    @Consumes(MediaType.WILDCARD)
    public Response post(@PathParam("service") String service,
                          @PathParam("path") String path,
                          byte[] body,
                          @Context HttpHeaders headers,
                          @Context UriInfo uriInfo) {
        return forward("POST", service, path, body, headers, uriInfo);
    }

    @PUT
    @Path("/{service}/{path: .*}")
    @Consumes(MediaType.WILDCARD)
    public Response put(@PathParam("service") String service,
                         @PathParam("path") String path,
                         byte[] body,
                         @Context HttpHeaders headers,
                         @Context UriInfo uriInfo) {
        return forward("PUT", service, path, body, headers, uriInfo);
    }

    @PATCH
    @Path("/{service}/{path: .*}")
    @Consumes(MediaType.WILDCARD)
    public Response patch(@PathParam("service") String service,
                           @PathParam("path") String path,
                           byte[] body,
                           @Context HttpHeaders headers,
                           @Context UriInfo uriInfo) {
        return forward("PATCH", service, path, body, headers, uriInfo);
    }

    @DELETE
    @Path("/{service}/{path: .*}")
    @Consumes(MediaType.WILDCARD)
    public Response delete(@PathParam("service") String service,
                            @PathParam("path") String path,
                            @Context HttpHeaders headers,
                            @Context UriInfo uriInfo) {
        return forward("DELETE", service, path, null, headers, uriInfo);
    }

    @HEAD
    @Path("/{service}/{path: .*}")
    @Consumes(MediaType.WILDCARD)
    public Response head(@PathParam("service") String service,
                          @PathParam("path") String path,
                          @Context HttpHeaders headers,
                          @Context UriInfo uriInfo) {
        return forward("HEAD", service, path, null, headers, uriInfo);
    }

    private Response forward(String method, String service, String path, byte[] body,
                              HttpHeaders headers, UriInfo uriInfo) {
        LOG.infof("Gateway proxy %s service=%s path=%s", method, service, path);
        return gatewayProxyService.proxy(method, service, path, body, headers, uriInfo);
    }
}
