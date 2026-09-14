package com.bis.intelliguide.service.admin;

import com.bis.intelliguide.model.Chunk;
import com.bis.intelliguide.model.Lab;
import com.bis.intelliguide.model.Standard;
import com.bis.intelliguide.repository.LabRepository;
import com.bis.intelliguide.repository.StandardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.bis.intelliguide.dto.request.StandardBulkImportRequest;
import com.bis.intelliguide.dto.request.LabBulkImportRequest;
import com.bis.intelliguide.service.rag.IngestionService;
import com.bis.intelliguide.dto.request.LabBulkImportRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BulkImportService {

    private final StandardRepository standardRepository;
    private final LabRepository labRepository;
    private final PdfOcrService pdfOcrService;
    private final IngestionService ingestionService;
    //private final com.bis.intelliguide.service.rag.IngestionService ingestionService;

    // =========================================================
    // PDF BULK IMPORT
    // =========================================================

    public BulkImportResult importPdf(
            MultipartFile file,
            String type,
            String adminId
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is empty");
        }

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Type is required. Use 'standards' or 'labs'"
            );
        }

        String normalizedType = type.trim().toLowerCase();

        if (!normalizedType.equals("standards")
                && !normalizedType.equals("labs")) {
            throw new IllegalArgumentException(
                    "Invalid type. Use 'standards' or 'labs'"
            );
        }

        // 1. OCR
        String extractedText = pdfOcrService.extractText(file);

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException(
                    "No text could be extracted from the PDF"
            );
        }

        // 2. Parse + Save
        if (normalizedType.equals("standards")) {

            List<Standard> standards =
                    parseStandards(extractedText, adminId);

            if (!standards.isEmpty()) {
                for (Standard s : standards) {
                    String textToEmbed = (s.getTitle() != null ? s.getTitle() + ". " : "")
                            + (s.getScope() != null ? s.getScope() : "");
                    if (!textToEmbed.isBlank()) {
                        s.setChunks(ingestionService.ingest(textToEmbed, s.getIsNumber()));
                    }
                }
                standardRepository.saveAll(standards);
            }

            return new BulkImportResult(
                    "standards",
                    extractedText.length(),
                    standards.size()
            );
        }

        List<Lab> labs = parseLabs(extractedText);

        if (!labs.isEmpty()) {
            labRepository.saveAll(labs);
        }

        return new BulkImportResult(
                "labs",
                extractedText.length(),
                labs.size()
        );
    }
    // =========================================================
    // JSON BULK IMPORT (structured, admin-authored — not OCR'd from a PDF)
    // =========================================================

    public int importStandardsJson(List<StandardBulkImportRequest> items, String adminId) {

        List<Standard> toSave = new ArrayList<>();

        for (StandardBulkImportRequest item : items) {

            if (item.getIsNumber() == null || item.getIsNumber().isBlank()) {
                continue; // isNumber is the minimum required field
            }

            String status = (item.getStatus() == null || item.getStatus().isBlank())
                    ? "DRAFT"
                    : item.getStatus().trim().toUpperCase();

            Standard standard = Standard.builder()
                    .isNumber(item.getIsNumber())
                    .title(item.getTitle())
                    .scope(item.getScope())
                    .category(item.getCategory())
                    .revision(item.getRevision())
                    .status(status)
                    .documentUrl(item.getDocumentUrl())
                    .regulatoryType(item.getRegulatoryType())
                    .isCompulsory(item.isCompulsory())
                    .certificationSchemeRef(item.getCertificationSchemeRef())
                    .version(1)
                    .createdBy(adminId)
                    .lastEditedBy(adminId)
                    .chunks(buildChunksForStandard(item))
                    .build();

            if (status.equals("PUBLISHED")) {
                standard.setPublishedAt(java.time.Instant.now());
            }

            toSave.add(standard);
        }

        standardRepository.saveAll(toSave);

        return toSave.size();
    }
    private List<Chunk> buildChunksForStandard(StandardBulkImportRequest item) {
        if (item.getChunks() != null && !item.getChunks().isEmpty()) {
            return ingestionService.embedProvidedChunks(item.getChunks());
        }
        // Chunks nahi diye gaye — title+scope se hi auto-embed kar do (PDF-import jaisa)
        String textToEmbed = (item.getTitle() != null ? item.getTitle() + ". " : "")
                + (item.getScope() != null ? item.getScope() : "");
        if (textToEmbed.isBlank()) {
            return new ArrayList<>();
        }
        return ingestionService.ingest(textToEmbed, item.getIsNumber());
    }
    public int importLabsJson(List<LabBulkImportRequest> items) {

        List<Lab> toSave = new ArrayList<>();

        for (LabBulkImportRequest item : items) {

            if (item.getName() == null || item.getName().isBlank()
                    || item.getState() == null || item.getState().isBlank()) {
                continue; // sirf name aur state zaroori hain
            }

            String recognitionStatus = (item.getRecognitionStatus() == null || item.getRecognitionStatus().isBlank())
                    ? "RECOGNIZED"
                    : item.getRecognitionStatus().toUpperCase().contains("RECOGNIZ") ? "RECOGNIZED" : "SUSPENDED";

            toSave.add(Lab.builder()
                    .name(item.getName())
                    .state(item.getState())
                    .district(item.getDistrict())
                    .distanceMeta(0.0)
                    .scope(item.getScope())
                    .workingHours(item.getWorkingHours())
                    .recognitionStatus(recognitionStatus)
                    .standardsCovered(item.getStandardsCovered())
                    .address(item.getAddress())
                    .slNo(item.getSlNo())
                    .ownershipType(item.getOwnershipType())
                    .oslCode(item.getOslCode())
                    .recognitionValidUpTo(item.getRecognitionValidUpTo())
                    .remarks(item.getRemarks())
                    .build());
        }

        labRepository.saveAll(toSave);

        return toSave.size();
    }

    // =========================================================
    // STANDARD PARSER
    // =========================================================

    private List<Standard> parseStandards(
            String text,
            String adminId
    ) {

        List<Standard> standards = new ArrayList<>();

        String normalizedText = text
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        String[] lines = normalizedText.split("\n");

        Standard current = null;
        StringBuilder scopeBuilder = new StringBuilder();

        Pattern standardNumberPattern = Pattern.compile(
                "(?i)\\b(?:IS|I\\.?S\\.?)\\s*[-:]?\\s*" +
                        "([0-9]{3,6}(?:[-/][0-9A-Za-z]+)*(?:\\s*:\\s*[0-9]{4})?)"
        );

        for (String rawLine : lines) {

            String line = cleanLine(rawLine);

            if (line.isBlank()) {
                continue;
            }

            Matcher matcher = standardNumberPattern.matcher(line);

            // -------------------------------------------------
            // New standard detected
            // -------------------------------------------------

            if (matcher.find()) {

                if (current != null) {
                    current.setScope(
                            scopeBuilder.toString().trim()
                    );

                    standards.add(current);
                }

                String isNumber = matcher.group(1).trim();

                String remaining = line
                        .substring(matcher.end())
                        .trim();

                current = Standard.builder()
                        .isNumber(isNumber)
                        .title("")
                        .scope("")
                        .revision("")
                        .category("BIS STANDARD")
                        .status("DRAFT")
                        .version(1)
                        .createdBy(adminId)
                        .lastEditedBy(adminId)
                        .build();

                scopeBuilder = new StringBuilder();

                if (!remaining.isBlank()) {
                    current.setTitle(remaining);
                }

                continue;
            }

            // Ignore obvious page/header/footer lines
            if (isIgnorableLine(line)) {
                continue;
            }

            if (current == null) {
                continue;
            }

            String lower = line.toLowerCase();

            // -------------------------------------------------
            // Revision
            // -------------------------------------------------

            if (lower.startsWith("revision")
                    || lower.startsWith("rev.")) {

                String value = extractValue(line);

                if (!value.isBlank()) {
                    current.setRevision(value);
                }

                continue;
            }

            // -------------------------------------------------
            // Category
            // -------------------------------------------------

            if (lower.startsWith("category")) {

                String value = extractValue(line);

                if (!value.isBlank()) {
                    current.setCategory(value);
                }

                continue;
            }

            // -------------------------------------------------
            // Scope
            // -------------------------------------------------

            if (lower.startsWith("scope")) {

                String value = extractValue(line);

                if (!value.isBlank()) {
                    scopeBuilder.append(value).append(" ");
                }

                continue;
            }

            // -------------------------------------------------
            // If title is empty, use current line as title
            // -------------------------------------------------

            if (current.getTitle() == null
                    || current.getTitle().isBlank()) {

                current.setTitle(line);
                continue;
            }

            // -------------------------------------------------
            // Otherwise treat additional text as scope
            // -------------------------------------------------

            if (scopeBuilder.length() < 5000) {
                scopeBuilder.append(line).append(" ");
            }
        }

        // Add last record
        if (current != null) {

            current.setScope(
                    scopeBuilder.toString().trim()
            );

            standards.add(current);
        }

        return standards;
    }

    // =========================================================
    // LAB PARSER
    // =========================================================

    private List<Lab> parseLabs(String text) {

        List<Lab> labs = new ArrayList<>();

        String normalizedText = text
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        String[] lines = normalizedText.split("\n");

        for (String rawLine : lines) {

            String line = cleanLine(rawLine);

            if (line.isBlank()) {
                continue;
            }

            if (isLabHeader(line)) {
                continue;
            }

            /*
             * OCR generally keeps table columns separated by
             * multiple spaces or tabs.
             */
            String[] columns = line.split("\\t+|\\s{2,}");

            if (columns.length < 3) {
                continue;
            }

            String name = safe(columns, 0);
            String state = safe(columns, 1);
            String district = safe(columns, 2);

            if (name.isBlank()
                    || state.isBlank()
                    || district.isBlank()) {
                continue;
            }

            double distance = 0.0;

            if (columns.length > 3) {
                distance = parseDouble(
                        safe(columns, 3)
                );
            }

            String scope = columns.length > 4
                    ? safe(columns, 4)
                    : "";

            String workingHours = columns.length > 5
                    ? safe(columns, 5)
                    : "";

            String recognitionStatus = columns.length > 6
                    ? safe(columns, 6)
                    : "RECOGNIZED";

            if (recognitionStatus.isBlank()) {
                recognitionStatus = "RECOGNIZED";
            }

            labs.add(
                    Lab.builder()
                            .name(name)
                            .state(state)
                            .district(district)
                            .distanceMeta(distance)
                            .scope(scope)
                            .workingHours(workingHours)
                            .recognitionStatus(
                                    recognitionStatus.toUpperCase()
                            )
                            .build()
            );
        }

        return labs;
    }

    // =========================================================
    // CSV IMPORT - EXISTING FUNCTIONALITY
    // =========================================================

    public int importStandardsCsv(
            MultipartFile file,
            String adminId
    ) throws IOException {

        List<Standard> toSave = new ArrayList<>();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     file.getInputStream(),
                                     StandardCharsets.UTF_8
                             ))) {

            String line;
            boolean first = true;

            while ((line = reader.readLine()) != null) {

                if (first) {
                    first = false;
                    continue;
                }

                if (line.isBlank()) {
                    continue;
                }

                String[] cols = line.split(",", -1);

                toSave.add(
                        Standard.builder()
                                .isNumber(safe(cols, 0))
                                .title(safe(cols, 1))
                                .scope(safe(cols, 2))
                                .revision(safe(cols, 3))
                                .category(safe(cols, 4))
                                .status("DRAFT")
                                .version(1)
                                .createdBy(adminId)
                                .lastEditedBy(adminId)
                                .build()
                );
            }
        }

        standardRepository.saveAll(toSave);

        return toSave.size();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String cleanLine(String line) {

        return line
                .replace("\u0000", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractValue(String line) {

        int colonIndex = line.indexOf(":");

        if (colonIndex >= 0
                && colonIndex + 1 < line.length()) {

            return line.substring(colonIndex + 1).trim();
        }

        String[] parts = line.split("\\s+", 2);

        if (parts.length == 2) {
            return parts[1].trim();
        }

        return "";
    }

    private boolean isIgnorableLine(String line) {

        String lower = line.toLowerCase();

        return lower.matches("page\\s+\\d+.*")
                || lower.contains("bureau of indian standards")
                || lower.equals("standards")
                || lower.equals("is number")
                || lower.equals("title")
                || lower.startsWith("contents");
    }

    private boolean isLabHeader(String line) {

        String lower = line.toLowerCase();

        return lower.contains("laboratory")
                && (lower.contains("name")
                || lower.contains("state"))
                || lower.equals("name state district")
                || lower.startsWith("lab name");
    }

    private double parseDouble(String value) {

        try {

            String cleaned = value
                    .replaceAll("[^0-9.]", "");

            if (cleaned.isBlank()) {
                return 0.0;
            }

            return Double.parseDouble(cleaned);

        } catch (Exception e) {
            return 0.0;
        }
    }

    private String safe(String[] arr, int index) {

        if (index >= arr.length) {
            return "";
        }

        return arr[index].trim();
    }

    // =========================================================
    // RESULT DTO
    // =========================================================

    public record BulkImportResult(
            String type,
            int ocrTextLength,
            int importedCount
    ) {
    }
    // =========================================================
    // DEDUPE HELPER
    // =========================================================

    public void deleteStandardById(String id) {
        standardRepository.deleteById(id);
    }
}