package com.myshop.model;

import java.time.Instant;

public record HistoryEntry(String query, Instant occurredAt) {
}
