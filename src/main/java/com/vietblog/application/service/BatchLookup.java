package com.vietblog.application.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared helpers for converting batched JPA aggregate/projection rows into lookup maps. */
final class BatchLookup {
    private BatchLookup() {
    }

    static Map<String, Long> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new HashMap<>(rows.size() * 2);
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] instanceof Number number) {
                result.put((String) row[0], number.longValue());
            }
        }
        return result;
    }

    static Map<String, String> toStringMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>(rows.size() * 2);
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                result.put((String) row[0], (String) row[1]);
            }
        }
        return result;
    }
}
