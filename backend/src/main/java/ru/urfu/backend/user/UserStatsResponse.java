package ru.urfu.backend.user;

import java.math.BigDecimal;

public record UserStatsResponse(BigDecimal savedKg, long exchangesCount, long activeListingsCount, long listingsCount) {
}
