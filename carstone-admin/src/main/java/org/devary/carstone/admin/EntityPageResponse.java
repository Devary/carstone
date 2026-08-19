package org.devary.carstone.admin;

import org.devary.table.TableContext;

import java.util.List;
import java.util.Map;

/**
 * Wire shape for a paginated {@code GET /{entity}} response. Ported from quar-crud-host verbatim.
 */
public record EntityPageResponse(long total, List<Map<String, Object>> items, TableContext context) {
}
