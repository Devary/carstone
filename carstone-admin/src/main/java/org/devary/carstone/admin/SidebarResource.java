package org.devary.carstone.admin;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.devary.table.sidebar.SidebarContext;

@Path("/sidebar")
@Produces(MediaType.APPLICATION_JSON)
public class SidebarResource {

    @Inject
    SidebarRegistry registry;

    @GET
    @Path("/{name}")
    public SidebarContext get(@PathParam("name") String name) {
        return registry.byName(name)
                .orElseThrow(() -> new NotFoundException("No sidebar named '" + name + "'"));
    }
}
