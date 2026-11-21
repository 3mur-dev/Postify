package com.omar.postify.dto.admin;

import com.omar.postify.dto.enums.DashboardSection;
import com.omar.postify.dto.enums.RangePreset;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Query parameters for the admin dashboard overview.
 */
public record AdminDashboardRequest(

        @NotNull(message = "Range preset is required")
        RangePreset rangePreset,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDateTime fromDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDateTime toDate,

        @NotNull(message = "Timezone is required")
        ZoneId timezone,

        @NotNull(message = "At least one dashboard section is required")
        Set<DashboardSection> sections

) {

    private static final long MAX_CUSTOM_RANGE_DAYS = 365;

    @AssertTrue(message = "Custom ranges require both fromDate and toDate")
    public boolean isCustomRangeComplete() {
        return rangePreset != RangePreset.CUSTOM
                || (fromDate != null && toDate != null);
    }

    @AssertTrue(message = "fromDate must be before or equal to toDate")
    public boolean isDateRangeValid() {
        return fromDate == null
                || toDate == null
                || !fromDate.isAfter(toDate);
    }

    @AssertTrue(message = "Dates cannot be in the future")
    public boolean isNotFutureRange() {
        LocalDateTime now = LocalDateTime.now();

        return (fromDate == null || !fromDate.isAfter(now))
                && (toDate == null || !toDate.isAfter(now));
    }

    @AssertTrue(message = "Custom date range cannot exceed 365 days")
    public boolean isRangeWithinLimit() {
        if (rangePreset != RangePreset.CUSTOM
                || fromDate == null
                || toDate == null) {
            return true;
        }

        return ChronoUnit.DAYS.between(fromDate, toDate)
                <= MAX_CUSTOM_RANGE_DAYS;
    }
}
