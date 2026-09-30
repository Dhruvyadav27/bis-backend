package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.model.Lab;
import com.bis.intelliguide.model.LabGeolocation;
import com.bis.intelliguide.repository.LabGeolocationRepository;
import com.bis.intelliguide.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final LabRepository labRepository;
    private final LabGeolocationRepository labGeolocationRepository;

    /**
     * One-time admin-triggered enrichment job.
     * For each lab not yet in lab_geolocation, calls OSM Nominatim to geocode.
     * Respects 1 req/sec rate limit (Nominatim policy).
     */
    public Map<String, Object> enrichLabGeolocation() {
        List<Lab> allLabs = labRepository.findAll();
        RestTemplate restTemplate = new RestTemplate();

        int processed = 0;
        int skipped = 0;
        int failed = 0;
        List<String> failedLabNames = new ArrayList<>();

        for (Lab lab : allLabs) {
            if (labGeolocationRepository.existsByLabName(lab.getName())) {
                skipped++;
                continue;
            }

            try {
                String query = lab.getName() + ", " + lab.getDistrict() + ", " + lab.getState() + ", India";
                String url = "https://nominatim.openstreetmap.org/search?q=" 
                        + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                        + "&format=json&limit=1&countrycodes=in";

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "BIS-IntelliGuide/1.0 (bis-intelliguide-project)");
                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = response.getBody();

                if (results != null && !results.isEmpty()) {
                    Map<String, Object> first = results.get(0);
                    double lat = Double.parseDouble(first.get("lat").toString());
                    double lon = Double.parseDouble(first.get("lon").toString());
                    String displayName = first.get("display_name").toString();

                    LabGeolocation geo = LabGeolocation.builder()
                            .labName(lab.getName())
                            .state(lab.getState())
                            .district(lab.getDistrict())
                            .city(lab.getDistrict())
                            .address(displayName)
                            .location(LabGeolocation.GeoPoint.builder()
                                    .type("Point")
                                    .coordinates(new double[]{lon, lat})
                                    .build())
                            .build();
                    labGeolocationRepository.save(geo);
                    processed++;
                } else {
                    log.warn("No geocoding result for lab: {}", lab.getName());
                    failed++;
                    failedLabNames.add(lab.getName());
                }

                // Rate limit: 1 request per second (Nominatim policy)
                Thread.sleep(1100);
            } catch (Exception e) {
                log.error("Geocoding failed for lab: {}", lab.getName(), e);
                failed++;
                failedLabNames.add(lab.getName());
            }
        }

        return Map.of(
                "processed", processed,
                "skipped", skipped,
                "failed", failed,
                "failedLabNames", failedLabNames
        );
    }
}
