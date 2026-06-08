package com.runpack.api.controller;

import com.runpack.api.dto.response.RunDetailResponse;
import com.runpack.api.dto.response.RunSummaryResponse;
import com.runpack.api.security.CurrentUser;
import com.runpack.api.service.HistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/users/me/runs")
    public Page<RunSummaryResponse> getMyRuns(
            @CurrentUser UUID currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return historyService.getRunHistory(currentUserId, PageRequest.of(page, size));
    }

    @GetMapping("/runs/{sessionId}")
    public RunDetailResponse getRunDetail(@CurrentUser UUID currentUserId,
                                          @PathVariable UUID sessionId) {
        return historyService.getRunDetail(sessionId, currentUserId);
    }
}
