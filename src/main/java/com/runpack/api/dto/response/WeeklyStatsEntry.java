package com.runpack.api.dto.response;

import java.time.LocalDate;

public record WeeklyStatsEntry(
    LocalDate weekStart,
    double totalDistanceM,
    int totalRuns
) {}
