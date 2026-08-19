package org.devary.carstone.admin;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.devary.table.TableContext;
import org.devary.table.crud.CrudHandler;
import org.devary.table.crud.ListQuery;
import org.devary.table.crud.PagedResult;
import org.devary.table.validation.EntityValidator;
import org.devary.table.validation.ValidationError;
import org.devary.table.validation.ValidationMessages;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generic CRUD endpoints for every entity that has a registered context. Ported verbatim from
 * quar-crud-host (package renamed) — every carstone entity is annotation-driven with a real
 * {@code @CrudstoneResource} handler, but this still falls back to {@link EntityStore} exactly
 * like the reference does, so this class needs no restructuring at all.
 */
@Path("/{entity}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EntityResource {

    // instantiated directly: context-gen is consumed as a plain library, its beans are not indexed
    private final EntityValidator validator = new EntityValidator();

    @Inject
    ContextRegistry registry;

    @Inject
    EntityStore store;

    @Inject
    HandlerRegistry handlers;

    private TableContext resolveContext(String entity) {
        return registry.byKeyOrEntityName(entity)
                .orElseThrow(() -> new NotFoundException("Unknown entity '" + entity + "'"));
    }

    private void requireWritable(TableContext context) {
        if (Boolean.TRUE.equals(context.getReadOnly())) {
            throw new ForbiddenException("'" + context.getName() + "' is read-only");
        }
    }

    private void requireValid(TableContext context, Map<String, Object> body) {
        List<ValidationError> errors = validator.validate(context, body);
        if (!errors.isEmpty()) {
            String message = errors.stream().map(ValidationMessages::format).collect(Collectors.joining("; "));
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(message)
                    .type(MediaType.TEXT_PLAIN)
                    .build());
        }
    }

    @GET
    public Uni<Response> all(@PathParam("entity") String entityParam,
                              @QueryParam("from") Integer from,
                              @QueryParam("to") Integer to,
                              @QueryParam("sort") String sortField,
                              @QueryParam("order") String order,
                              @QueryParam("q") String q,
                              @QueryParam("includeContext") boolean includeContext,
                              @Context UriInfo uriInfo) {
        TableContext context = resolveContext(entityParam);
        String entity = context.getName();
        if (from == null || to == null) {
            Uni<List<Map<String, Object>>> all = handlers.forEntity(entity)
                    .map(CrudHandler::list)
                    .orElseGet(() -> Uni.createFrom().item(store.all(entity)));
            return all.onItem().transform(items -> Response.ok(items).build());
        }

        ListQuery query = new ListQuery(from, to, sortField, "desc".equalsIgnoreCase(order), q, parseFilters(uriInfo));
        Uni<PagedResult> paged = handlers.forEntity(entity)
                .map(handler -> handler.query(query))
                .orElseGet(() -> Uni.createFrom().item(store.query(entity, query)));
        return paged.onItem().transform(result ->
                Response.ok(new EntityPageResponse(result.total(), result.items(), includeContext ? context : null)).build());
    }

    private static Map<String, List<String>> parseFilters(UriInfo uriInfo) {
        Map<String, List<String>> filters = new LinkedHashMap<>();
        uriInfo.getQueryParameters().forEach((key, values) -> {
            if (key.startsWith("filter.") && !values.isEmpty()) {
                String field = key.substring("filter.".length());
                List<String> flattened = values.stream()
                        .flatMap(v -> List.of(v.split(",")).stream())
                        .filter(v -> !v.isBlank())
                        .toList();
                if (!flattened.isEmpty()) {
                    filters.put(field, flattened);
                }
            }
        });
        return filters;
    }

    @GET
    @Path("/{id}")
    public Uni<Map<String, Object>> one(@PathParam("entity") String entityParam, @PathParam("id") String id) {
        String entity = resolveContext(entityParam).getName();
        Uni<Optional<Map<String, Object>>> result = handlers.forEntity(entity)
                .map(handler -> handler.get(id))
                .orElseGet(() -> Uni.createFrom().item(store.one(entity, id)));
        return result.onItem().transform(opt -> opt.orElseThrow(() -> new NotFoundException("No " + entity + " with id " + id)));
    }

    @POST
    public Uni<Response> create(@PathParam("entity") String entityParam,
                                 @QueryParam("validateOnly") boolean validateOnly,
                                 Map<String, Object> body) {
        TableContext context = resolveContext(entityParam);
        String entity = context.getName();
        if (validateOnly) {
            return Uni.createFrom().item(Response.ok(validator.validate(context, body)).build());
        }
        requireWritable(context);
        requireValid(context, body);
        Uni<Map<String, Object>> created = handlers.forEntity(entity)
                .map(handler -> handler.create(body))
                .orElseGet(() -> Uni.createFrom().item(store.create(entity, body)));
        return created.onItem().transform(c -> Response.status(Response.Status.CREATED).entity(c).build());
    }

    @PUT
    @Path("/{id}")
    public Uni<Map<String, Object>> update(@PathParam("entity") String entityParam, @PathParam("id") String id,
                                            Map<String, Object> body) {
        TableContext context = resolveContext(entityParam);
        String entity = context.getName();
        requireWritable(context);
        requireValid(context, body);
        Uni<Optional<Map<String, Object>>> result = handlers.forEntity(entity)
                .map(handler -> handler.update(id, body))
                .orElseGet(() -> Uni.createFrom().item(store.update(entity, id, body)));
        return result.onItem().transform(opt -> opt.orElseThrow(() -> new NotFoundException("No " + entity + " with id " + id)));
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("entity") String entityParam, @PathParam("id") String id) {
        TableContext context = resolveContext(entityParam);
        requireWritable(context);
        String entity = context.getName();
        Uni<Boolean> deleted = handlers.forEntity(entity)
                .map(handler -> handler.delete(id))
                .orElseGet(() -> Uni.createFrom().item(store.delete(entity, id)));
        return deleted.onItem().transform(ok -> {
            if (!ok) {
                throw new NotFoundException("No " + entity + " with id " + id);
            }
            return Response.noContent().build();
        });
    }

    @DELETE
    public Uni<Map<String, Integer>> deleteMany(@PathParam("entity") String entityParam, List<String> ids) {
        TableContext context = resolveContext(entityParam);
        requireWritable(context);
        String entity = context.getName();
        if (ids == null || ids.isEmpty()) {
            return Uni.createFrom().item(Map.of("deleted", 0));
        }
        Uni<Integer> deleted = handlers.forEntity(entity)
                .map(handler -> handler.deleteMany(ids))
                .orElseGet(() -> Uni.createFrom().item(store.deleteMany(entity, ids)));
        return deleted.onItem().transform(count -> Map.of("deleted", count));
    }
}
