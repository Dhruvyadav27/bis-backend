package com.bis.intelliguide.config;

import com.bis.intelliguide.model.HuidRecord;
import com.bis.intelliguide.model.Lab;
import com.bis.intelliguide.repository.HuidRecordRepository;
import com.bis.intelliguide.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Seeds a handful of demo records (HUID + Labs) so /api/hallmarking/verify-huid and
 * /api/labs/search return real data out of the box for a hackathon demo, without
 * waiting on a real BIS data feed. Runs after the admin-seed CommandLineRunner.
 */
@Component
@RequiredArgsConstructor
@Order(2)
public class DataSeeder implements CommandLineRunner {

    private final HuidRecordRepository huidRecordRepository;
    private final LabRepository labRepository;
    private final com.bis.intelliguide.repository.LabGeolocationRepository labGeolocationRepository;

    @Override
    public void run(String... args) {
        if (huidRecordRepository.count() == 0) {
            huidRecordRepository.save(HuidRecord.builder()
                    .huid("AB12CD").purity("916 (22K)").ahcCentre("Bhopal AHC Center 1")
                    .hallmarkedOn(Instant.parse("2025-03-10T00:00:00Z")).build());
            huidRecordRepository.save(HuidRecord.builder()
                    .huid("XY99ZZ").purity("750 (18K)").ahcCentre("Indore AHC Center 2")
                    .hallmarkedOn(Instant.parse("2025-06-21T00:00:00Z")).build());
        }

        if (labRepository.count() == 0) {
            labRepository.save(Lab.builder()
                    .name("BIS Recognized Lab, Bhopal").state("Madhya Pradesh").district("Bhopal")
                    .distanceMeta(3.2).scope("Electrical safety, LED lighting")
                    .workingHours("10:00-18:00 Mon-Sat").recognitionStatus("RECOGNIZED").build());
            labRepository.save(Lab.builder()
                    .name("Central Testing Lab, Indore").state("Madhya Pradesh").district("Indore")
                    .distanceMeta(190.5).scope("Textiles, helmets, general safety")
                    .workingHours("09:30-17:30 Mon-Fri").recognitionStatus("RECOGNIZED").build());
        }

        if (labGeolocationRepository.count() == 0) {
            labGeolocationRepository.saveAll(java.util.List.of(
                com.bis.intelliguide.model.LabGeolocation.builder()
                    .labName("BIS Recognized Lab, Bhopal")
                    .state("Madhya Pradesh").district("Bhopal").city("Bhopal")
                    .address("E-5, Arera Colony, Bhopal")
                    .location(com.bis.intelliguide.model.LabGeolocation.GeoPoint.builder().type("Point").coordinates(new double[]{77.4340, 23.2330}).build())
                    .build(),
                com.bis.intelliguide.model.LabGeolocation.builder()
                    .labName("Central Testing Lab, Indore")
                    .state("Madhya Pradesh").district("Indore").city("Indore")
                    .address("Vijay Nagar, Indore")
                    .location(com.bis.intelliguide.model.LabGeolocation.GeoPoint.builder().type("Point").coordinates(new double[]{75.8577, 22.7196}).build())
                    .build()
            ));
            System.out.println("[SEED] Seeded 2 lab geolocation records.");
        }
    }
}
