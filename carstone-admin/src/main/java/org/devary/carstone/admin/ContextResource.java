package org.devary.carstone.admin;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.devary.table.TableContext;
import org.devary.table.search.SearchContext;

import java.util.Set;

@Path("/context")
@Produces(MediaType.APPLICATION_JSON)
public class ContextResource {

    @Inject
    ContextRegistry registry;

    @Inject
    SearchContextRegistry searchRegistry;

    @GET
    public Set<String> list() {
        return registry.keys();
    }

    @GET
    @Path("/{name}")
    public TableContext get(@PathParam("name") String name) {
        return registry.byKeyOrEntityName(name)
                .orElseThrow(() -> new NotFoundException("No context named '" + name + "'"));
    }

    @GET
    @Path("/{name}/search")
    public SearchContext getSearch(@PathParam("name") String name) {
        return searchRegistry.byEntityName(name)
                .orElseThrow(() -> new NotFoundException("No search context for '" + name + "'"));
    }
}
