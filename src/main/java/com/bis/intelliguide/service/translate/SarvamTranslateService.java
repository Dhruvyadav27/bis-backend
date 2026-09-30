package com.bis.intelliguide.service.translate;

import com.bis.intelliguide.config.LanguageContext;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sarvam AI translation. Never throws: if Sarvam is down, unconfigured or rate-limited the
 * original text is returned unchanged, so the app degrades to English instead of failing.
 *
 *  - toEnglish():   Mayura (auto-detects the source, copes with Hinglish/code-mixed), 1000-char limit
 *  - fromEnglish(): Sarvam-Translate (formal, all 22 languages), 2000-char limit -> long text is chunked
 * Results are cached (LRU), so fixed messages and repeated clauses cost one API call per language.
 */
@Service
public class SarvamTranslateService {

    private static final Logger log = LoggerFactory.getLogger(SarvamTranslateService.class);

    private static final String MODEL_LONG = "sarvam-translate:v1";
    private static final String MODEL_QUERY = "mayura:v1";
    private static final int MAX_LONG = 1800;   // API limit 2000
    private static final int MAX_QUERY = 950;   // API limit 1000
    private static final int CACHE_SIZE = 1000;
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient client;
    private final boolean enabled;
    private final Map<String, String> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    public SarvamTranslateService(@Value("${app.sarvam.api-key:}") String apiKey,
                                  @Value("${app.sarvam.base-url:https://api.sarvam.ai}") String baseUrl) {
        this.enabled = apiKey != null && !apiKey.isBlank();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(20_000);
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("api-subscription-key", apiKey == null ? "" : apiKey)
                .build();
        if (!enabled) {
            log.warn("SARVAM_API_KEY is not set: translation is disabled, every answer will stay in English.");
        }
    }

    // ---------- public API ----------

    /** User's language -> English (for retrieval and the LLM). No-op for English users. */
    public String toEnglish(String text) {
        if (isBlank(text) || !enabled || LanguageContext.isEnglish()) return text;
        if (cache.containsKey("EN|" + text)) return text; // already an English result we produced

        String english = text.length() <= MAX_QUERY
                ? call(text, "auto", "en-IN", MODEL_QUERY)
                : translateLong(text, LanguageContext.sarvamCode(), "en-IN");
        if (english == null) return text;
        cache.put("EN|" + english, english);
        return english;
    }

    /** English -> user's language (answers, fixed messages). No-op for English users. */
    public String fromEnglish(String text) {
        if (isBlank(text) || !enabled || LanguageContext.isEnglish()) return text;
        String out = translateLong(text, "en-IN", LanguageContext.sarvamCode());
        return out != null ? out : text;
    }

    /** Translates a list in parallel, keeping the order. */
    public List<String> fromEnglishAll(List<String> items) {
        if (items == null || items.isEmpty() || !enabled || LanguageContext.isEnglish()) return items;
        final String target = LanguageContext.sarvamCode(); // resolved here: worker threads don't see the ThreadLocal
        return items.parallelStream()
                .map(s -> {
                    if (isBlank(s)) return s;
                    String out = translateLong(s, "en-IN", target);
                    return out != null ? out : s;
                })
                .toList();
    }

    // ---------- internals ----------

    /** Chunks text under the API limit; returns null if any chunk fails (so a half-translated mix is never cached). */
    private String translateLong(String text, String src, String tgt) {
        String cacheKey = key(MODEL_LONG, src, tgt, text);
        String hit = cache.get(cacheKey);
        if (hit != null) return hit;

        StringBuilder out = new StringBuilder();
        for (String chunk : chunk(text)) {
            String trimmed = chunk.strip();
            if (trimmed.isEmpty()) {
                out.append(chunk);
                continue;
            }
            String translated = call(trimmed, src, tgt, MODEL_LONG);
            if (translated == null) return null;
            out.append(translated).append(chunk.endsWith("\n") ? "\n" : " ");
        }
        String result = out.toString().strip();
        cache.put(cacheKey, result);
        return result;
    }

    private List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String piece : text.split("(?<=[.!?।\\n])")) {
            while (piece.length() > MAX_LONG) { // one enormous sentence: hard cut
                if (cur.length() > 0) {
                    chunks.add(cur.toString());
                    cur.setLength(0);
                }
                chunks.add(piece.substring(0, MAX_LONG));
                piece = piece.substring(MAX_LONG);
            }
            if (cur.length() + piece.length() > MAX_LONG) {
                chunks.add(cur.toString());
                cur.setLength(0);
            }
            cur.append(piece);
        }
        if (cur.length() > 0) chunks.add(cur.toString());
        return chunks;
    }

    /** One Sarvam call with retry on 429/5xx/timeouts. Returns null on failure. */
    private String call(String input, String src, String tgt, String model) {
        String cacheKey = key(model, src, tgt, input);
        String hit = cache.get(cacheKey);
        if (hit != null) return hit;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("input", input);
        body.put("source_language_code", src);
        body.put("target_language_code", tgt);
        body.put("model", model);
        body.put("mode", "formal");
        body.put("numerals_format", "international");

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                SarvamResponse r = client.post().uri("/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(SarvamResponse.class);
                if (r == null || isBlank(r.translatedText())) return null;
                cache.put(cacheKey, r.translatedText());
                return r.translatedText();
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    log.warn("Sarvam translate failed ({}): {}", status, e.getResponseBodyAsString());
                    return null;
                }
                pause(attempt);
            } catch (ResourceAccessException e) { // timeout / connection problem
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Sarvam translate unreachable: {}", e.getMessage());
                    return null;
                }
                pause(attempt);
            } catch (Exception e) {
                log.warn("Sarvam translate error: {}", e.toString());
                return null;
            }
        }
        return null;
    }

    private static void pause(int attempt) {
        try {
            Thread.sleep(800L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static String key(String model, String src, String tgt, String text) {
        return model + "|" + src + "|" + tgt + "|" + text;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SarvamResponse(@JsonProperty("translated_text") String translatedText) {}
}