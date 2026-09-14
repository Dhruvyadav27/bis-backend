package com.bis.intelliguide.controller.admin;

import com.bis.intelliguide.dto.response.ConsumerTrendsResponse;
import com.bis.intelliguide.model.Complaint;
import com.bis.intelliguide.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregated-only: counts by category/date. Deliberately exposes NO endpoint here
 * that returns an individual user's complaint details — that lives under
 * /api/admin/standards.../ equivalents is not applicable; individual complaint lookup
 * for support purposes should go through a dedicated, explicitly-audited endpoint,
 * not this trends endpoint.
 */
@RestController
@RequestMapping("/api/admin/consumer-trends")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConsumerTrendsController {

    private final ComplaintRepository complaintRepository;

    @GetMapping
    public ConsumerTrendsResponse trends() {
        List<Complaint> all = complaintRepository.findAll();

        Map<String, Long> byCategory = all.stream()
                .collect(Collectors.groupingBy(Complaint::getComplaintType, Collectors.counting()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> byDate = all.stream()
                .filter(c -> c.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getCreatedAt().atZone(ZoneOffset.UTC).format(fmt),
                        Collectors.counting()));

        return ConsumerTrendsResponse.builder()
                .categories(byCategory.entrySet().stream()
                        .map(e -> new ConsumerTrendsResponse.CategoryCount(e.getKey(), e.getValue()))
                        .toList())
                .trend(byDate.entrySet().stream()
                        .map(e -> new ConsumerTrendsResponse.DateCount(e.getKey(), e.getValue()))
                        .toList())
                .build();
    }
}
