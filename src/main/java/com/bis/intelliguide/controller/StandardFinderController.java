package com.bis.intelliguide.controller;

import com.bis.intelliguide.dto.request.StandardSearchRequest;
import com.bis.intelliguide.dto.response.ClauseTextResponse;
import com.bis.intelliguide.dto.response.StandardSearchResponse;
import com.bis.intelliguide.model.Chunk;
import com.bis.intelliguide.repository.BisServiceRepository;
import com.bis.intelliguide.repository.CertificationSchemeRepository;
import com.bis.intelliguide.repository.ConsumerRuleRepository;
import com.bis.intelliguide.repository.StandardRepository;
import com.bis.intelliguide.service.agents.StandardFinderService;
import com.bis.intelliguide.service.translate.SarvamTranslateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/standards")
@RequiredArgsConstructor
public class StandardFinderController {

    private final StandardFinderService standardFinderService;
    private final StandardRepository standardRepository;
    private final CertificationSchemeRepository schemeRepository;
    private final BisServiceRepository bisServiceRepository;
    private final ConsumerRuleRepository consumerRuleRepository;
    private final SarvamTranslateService translator;

    @PostMapping("/search")
    public ResponseEntity<StandardSearchResponse> search(@Valid @RequestBody StandardSearchRequest request,
                                                         Authentication authentication) {
        String userId = authentication != null ? (String) authentication.getPrincipal() : null;
        return ResponseEntity.ok(standardFinderService.search(request.getProductDescription(), userId));
    }

    @GetMapping("/clause")
    public ResponseEntity<ClauseTextResponse> getClauseText(
            @RequestParam String doc,
            @RequestParam String clause) {

        Optional<com.bis.intelliguide.model.Standard> standardOpt = standardRepository.findByIsNumber(doc);
        if (standardOpt.isPresent()) {
            return chunkLookup(standardOpt.get().getChunks(), doc, standardOpt.get().getTitle(), clause);
        }

        var schemeOpt = schemeRepository.findBySchemeName(doc);
        if (schemeOpt.isPresent()) {
            return chunkLookup(schemeOpt.get().getChunks(), doc, schemeOpt.get().getSchemeName(), clause);
        }

        var serviceOpt = bisServiceRepository.findByServiceName(doc);
        if (serviceOpt.isPresent()) {
            return chunkLookup(serviceOpt.get().getChunks(), doc, serviceOpt.get().getServiceName(), clause);
        }

        var ruleOpt = consumerRuleRepository.findByActNameAndClauseRef(doc, clause);
        if (ruleOpt.isPresent()) {
            return ResponseEntity.ok(localize(ClauseTextResponse.builder()
                    .isNumber(doc)
                    .clauseRef(clause)
                    .docTitle(ruleOpt.get().getActName())
                    .text(ruleOpt.get().getDescription())
                    .build()));
        }

        return ResponseEntity.status(404).body(ClauseTextResponse.builder()
                .isNumber(doc).clauseRef(clause).docTitle(doc)
                .text("No stored text found for this reference.")
                .build());
    }

    private ResponseEntity<ClauseTextResponse> chunkLookup(
            List<Chunk> chunks, String doc, String title, String clause) {

        Chunk matched = chunks != null
                ? chunks.stream().filter(c -> clause.equalsIgnoreCase(c.getClauseRef())).findFirst().orElse(null)
                : null;

        if (matched == null) {
            return ResponseEntity.status(404).body(ClauseTextResponse.builder()
                    .isNumber(doc).clauseRef(clause).docTitle(title)
                    .text("No stored text found for this clause.")
                    .build());
        }

        return ResponseEntity.ok(localize(ClauseTextResponse.builder()
                .isNumber(doc).clauseRef(clause).docTitle(title).text(matched.getText())
                .build()));
    }

    /** Translates the clause into the user's language, keeping the English original alongside. */
    private ClauseTextResponse localize(ClauseTextResponse r) {
        String original = r.getText();
        String translated = translator.fromEnglish(original);
        if (translated != null && !translated.equals(original)) {
            r.setOriginalText(original);
            r.setText(translated);
            r.setTranslated(true);
        }
        return r;
    }
}