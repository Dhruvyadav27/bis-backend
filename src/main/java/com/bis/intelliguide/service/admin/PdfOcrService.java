package com.bis.intelliguide.service.admin;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfOcrService {

    // If a page's real text layer has at least this many characters, trust it and
    // skip OCR for that page entirely (text-layer extraction is far more accurate
    // than OCR, and instant).
    private static final int MIN_TEXT_LAYER_CHARS = 20;

    public String extractText(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        List<String> pagesText = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            ITesseract tesseract = null; // created lazily, only if a page actually needs OCR

            for (int page = 0; page < totalPages; page++) {

                // 1. Try the PDF's real text layer first (works for text-based PDFs,
                //    including most exported/printed tables — no OCR noise at all).
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(page + 1);
                stripper.setEndPage(page + 1);
                String layerText = stripper.getText(document);

                if (layerText != null && layerText.trim().length() >= MIN_TEXT_LAYER_CHARS) {
                    pagesText.add("===== PAGE " + (page + 1) + " =====\n" + layerText);
                    continue;
                }

                // 2. No usable text layer on this page (likely a scanned image) — fall
                //    back to OCR, rendered at a higher DPI to help Tesseract with small
                //    table text.
                if (tesseract == null) {
                    tesseract = createTesseract();
                }

                BufferedImage image = renderer.renderImageWithDPI(page, 400);

                String ocrText;
                try {
                    ocrText = tesseract.doOCR(image);
                } catch (TesseractException e) {
                    throw new RuntimeException("OCR failed while processing PDF page " + (page + 1), e);
                }

                if (ocrText != null && !ocrText.isBlank()) {
                    pagesText.add("===== PAGE " + (page + 1) + " (OCR) =====\n" + ocrText);
                }
            }

        }

        return String.join("\n\n", pagesText);
    }

    private ITesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(resolveTessdataPath());
        tesseract.setLanguage("eng");

        // PSM 6 = "assume a single uniform block of text". Works noticeably better
        // than the default (fully automatic page segmentation) on table-like layouts
        // with thin borders/rules, which is what was triggering the
        // "Image too small to scale" / "Line cannot be recognized" warnings.
        tesseract.setPageSegMode(6);

        return tesseract;
    }

    /**
     * Resolves the Tesseract tessdata directory without hardcoding a single OS path.
     * Order: TESSDATA_PREFIX env var -> common install locations per OS -> error.
     */
    private String resolveTessdataPath() {
        String envPath = System.getenv("TESSDATA_PREFIX");
        if (envPath != null && new File(envPath).isDirectory()) {
            return envPath;
        }

        String[] commonPaths = {
                "C:/Program Files/Tesseract-OCR/tessdata",
                "/usr/share/tesseract-ocr/5/tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/local/share/tessdata",
                "/opt/homebrew/share/tessdata",
        };

        for (String path : commonPaths) {
            if (new File(path).isDirectory()) {
                return path;
            }
        }

        throw new IllegalStateException(
                "Tesseract tessdata directory not found. Set the TESSDATA_PREFIX "
                        + "environment variable to your tessdata folder (e.g. the one "
                        + "containing eng.traineddata)."
        );
    }
}
