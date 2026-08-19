package org.devary.carstone.front;

import org.devary.table.TableContext;

import java.util.List;
import java.util.Map;

public record EntityPageResponse(long total, List<Map<String, Object>> items, TableContext context) {
}
