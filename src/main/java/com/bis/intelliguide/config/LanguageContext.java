package com.bis.intelliguide.config;

import java.util.Map;

/** Language of the request currently being handled (set by LanguageFilter). */
public final class LanguageContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    // browser/i18n code -> Sarvam code. The frontend ships a subset; the backend accepts all 22 scheduled languages.
    private static final Map<String, String> SARVAM = Map.ofEntries(
            Map.entry("en", "en-IN"), Map.entry("hi", "hi-IN"), Map.entry("bn", "bn-IN"),
            Map.entry("gu", "gu-IN"), Map.entry("kn", "kn-IN"), Map.entry("ml", "ml-IN"),
            Map.entry("mr", "mr-IN"), Map.entry("od", "od-IN"), Map.entry("or", "od-IN"),
            Map.entry("pa", "pa-IN"), Map.entry("ta", "ta-IN"), Map.entry("te", "te-IN"),
            Map.entry("as", "as-IN"), Map.entry("brx", "brx-IN"), Map.entry("doi", "doi-IN"),
            Map.entry("kok", "kok-IN"), Map.entry("ks", "ks-IN"), Map.entry("mai", "mai-IN"),
            Map.entry("mni", "mni-IN"), Map.entry("ne", "ne-IN"), Map.entry("sa", "sa-IN"),
            Map.entry("sat", "sat-IN"), Map.entry("sd", "sd-IN"), Map.entry("ur", "ur-IN"));

    private LanguageContext() {}

    public static void set(String code) { CURRENT.set(code); }
    public static void clear() { CURRENT.remove(); }

    public static String code() {
        String c = CURRENT.get();
        return c != null ? c : "en";
    }

    public static boolean isEnglish() { return "en".equals(code()); }

    public static String sarvamCode() { return SARVAM.getOrDefault(code(), "en-IN"); }

    /** "hi", "hi-IN,hi;q=0.9,en;q=0.8", "or" ... -> a supported primary code, defaulting to "en". */
    public static String parse(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) return "en";
        String first = acceptLanguage.split("[,;]")[0].trim().toLowerCase();
        String primary = first.split("[-_]")[0];
        return SARVAM.containsKey(primary) ? primary : "en";
    }
}