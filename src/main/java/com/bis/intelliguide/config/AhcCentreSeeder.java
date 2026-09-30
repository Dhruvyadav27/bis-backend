package com.bis.intelliguide.config;

import com.bis.intelliguide.model.AhcCentre;
import com.bis.intelliguide.repository.AhcCentreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the known real BIS AHC centres on first run (collection starts empty).
 * Add more real centres later via the admin bulk-import endpoint — this seeder
 * only ever fires once, when the collection is empty.
 */
@Component
@RequiredArgsConstructor
@Order(2) // after the admin-seed CommandLineRunner
public class AhcCentreSeeder implements CommandLineRunner {

    private final AhcCentreRepository ahcCentreRepository;

    @Override
    public void run(String... args) {
        if (ahcCentreRepository.count() > 0) {
            return;
        }

        List<AhcCentre> centres = List.of(
                centre("BIS AHC Bhopal", "Bhopal", "Madhya Pradesh",
                        "Paryavaran Parisar, E-5 Arera Colony, Bhopal 462016", 77.4340, 23.2330),
                centre("BIS AHC Delhi", "New Delhi", "Delhi",
                        "9 Bahadur Shah Zafar Marg, New Delhi 110002", 77.2410, 28.6280),
                centre("BIS AHC Mumbai", "Mumbai", "Maharashtra",
                        "MIDC, Andheri East, Mumbai 400093", 72.8682, 19.1125),
                centre("BIS AHC Chennai", "Chennai", "Tamil Nadu",
                        "CIT Campus, Taramani, Chennai 600113", 80.2270, 12.9870),
                centre("BIS AHC Kolkata", "Kolkata", "West Bengal",
                        "Block CP, Sector V, Salt Lake, Kolkata 700091", 88.4350, 22.5764),
                centre("BIS AHC Jaipur", "Jaipur", "Rajasthan",
                        "Institutional Area, Jhalana Dungri, Jaipur 302004", 75.8116, 26.8571)
        );

        ahcCentreRepository.saveAll(centres);
    }

    private AhcCentre centre(String name, String city, String state, String address, double lng, double lat) {
        return AhcCentre.builder()
                .name(name).city(city).state(state).address(address)
                .location(AhcCentre.GeoPoint.builder()
                        .type("Point")
                        .coordinates(new double[]{lng, lat})
                        .build())
                .build();
    }
}