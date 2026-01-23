package com.example.smart_booking_system.service.ai;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CityExtractor
 *
 * - extractCity(text): cố gắng tìm tỉnh/thành trong text (trả về tên chính thức có dấu, ví dụ "Hà Giang")
 * - normalizeForQuery(city): chuẩn hoá để dùng khi query DB (lowercase, no-diacritics, remove prefixes)
 *
 * Usage:
 *   String city = CityExtractor.extractCity(userText);
 *   if (city != null) {
 *       String q = CityExtractor.normalizeForQuery(city);
 *       propertyDetailRepository.findAvailableProperties(q, capacity);
 *   }
 */
public final class CityExtractor {

    // canonical display names (Vietnamese có dấu)
    private static final List<String> CANONICAL_NAMES = List.of(
            "Hà Nội","Hồ Chí Minh","An Giang","Bà Rịa - Vũng Tàu","Bắc Giang","Bắc Kạn",
            "Bạc Liêu","Bắc Ninh","Bến Tre","Bình Định","Bình Dương","Bình Phước",
            "Bình Thuận","Cà Mau","Cần Thơ","Cao Bằng","Đà Nẵng","Đắk Lắk","Đắk Nông",
            "Điện Biên","Đồng Nai","Đồng Tháp","Gia Lai","Hà Giang","Hà Nam","Hà Tĩnh",
            "Hải Dương","Hải Phòng","Hậu Giang","Hòa Bình","Hưng Yên","Khánh Hòa",
            "Kiên Giang","Kon Tum","Lai Châu","Lâm Đồng","Lạng Sơn","Lào Cai","Long An",
            "Nam Định","Nghệ An","Ninh Bình","Ninh Thuận","Phú Thọ","Phú Yên","Quảng Bình",
            "Quảng Nam","Quảng Ngãi","Quảng Ninh","Quảng Trị","Sóc Trăng","Sơn La",
            "Tây Ninh","Thái Bình","Thái Nguyên","Thanh Hóa","Thừa Thiên Huế","Tiền Giang",
            "Trà Vinh","Tuyên Quang","Vĩnh Long","Vĩnh Phúc","Yên Bái"
    );

    // map alias normalized -> canonical display name
    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    // precompute normalized canonical forms for faster fuzzy checks
    private static final Map<String, String> CANONICAL_NORMALIZED = new HashMap<>();

    // threshold for fuzzy similarity (0..1), higher = stricter
    private static final double FUZZY_THRESHOLD = 0.70;

    static {
        // fill canonical normalized
        for (String c : CANONICAL_NAMES) {
            CANONICAL_NORMALIZED.put(normalize(c), c);
        }

        // common aliases (normalized -> canonical)
        // Hà Nội
        registerAlias("ha noi", "Hà Nội");
        registerAlias("hn", "Hà Nội");
        registerAlias("hanoi", "Hà Nội");

        // Hồ Chí Minh / Sài Gòn
        registerAlias("hcm", "Hồ Chí Minh");
        registerAlias("tp hcm", "Hồ Chí Minh");
        registerAlias("hochiminh", "Hồ Chí Minh");
        registerAlias("ho chi minh", "Hồ Chí Minh");
        registerAlias("saigon", "Hồ Chí Minh");
        registerAlias("sai gon", "Hồ Chí Minh");
        registerAlias("sài gòn", "Hồ Chí Minh");
        registerAlias("tp hcm", "Hồ Chí Minh");

        // some provinces with common aliases
        registerAlias("ha giang", "Hà Giang");
        registerAlias("hagiang", "Hà Giang");

        registerAlias("quang ninh", "Quảng Ninh");
        registerAlias("quangninh", "Quảng Ninh");

        registerAlias("hai phong", "Hải Phòng");
        registerAlias("haiphong", "Hải Phòng");

        registerAlias("da nang", "Đà Nẵng");
        registerAlias("danang", "Đà Nẵng");
        registerAlias("da-nang", "Đà Nẵng");

        registerAlias("nha trang", "Khánh Hòa");
        registerAlias("khanh hoa", "Khánh Hòa");

        // add reasonable short forms / lowercase without diacritics / alternative words
        // iterate canonical names to add normalized forms automatically
        for (String canon : CANONICAL_NAMES) {
            String n = normalize(canon);
            registerAlias(n, canon);
            registerAlias(n.replaceAll("\\s+", ""), canon); // no-space form
        }

        // some multi-word hyphen forms
        registerAlias("ba ria vung tau", "Bà Rịa - Vũng Tàu");
        registerAlias("baria vungtau", "Bà Rịa - Vũng Tàu");
        registerAlias("ben tre", "Bến Tre");
        registerAlias("bac ninh", "Bắc Ninh");
        registerAlias("bac giang", "Bắc Giang");
        // ... you can add more aliases here if needed
    }

    private CityExtractor() { /* no instantiation */ }

    private static void registerAlias(String norm, String canonical) {
        if (norm == null || canonical == null) return;
        ALIAS_MAP.put(norm.toLowerCase(Locale.ROOT), canonical);
    }

    /**
     * Tries to extract a canonical city/province name from free text.
     * Returns canonical Vietnamese name with accents (e.g., "Hà Giang") or null if not found.
     */
    public static String extractCity(String text) {
        if (text == null || text.isBlank()) return null;
        String lower = text.trim().toLowerCase(Locale.ROOT);

        // 1) quick direct checks: look for "ở <city>" or "tại <city>"
        Matcher m = Pattern.compile("\\b(?:ở|tại|ở tại|ở ở|ở tp)\\s+([\\p{L}\\s\\-]+)", Pattern.CASE_INSENSITIVE).matcher(lower);
        if (m.find()) {
            String candidate = m.group(1).trim();
            candidate = candidate.replaceAll("\\b(nhé|ạ|giùm|giúp|đi|nhé)\\b.*$", "").trim();
            String found = findBestMatch(candidate);
            if (found != null) return found;
        }

        // 2) direct alias substring match
        for (Map.Entry<String, String> e : ALIAS_MAP.entrySet()) {
            String alias = e.getKey();
            if (lower.contains(alias)) {
                return e.getValue();
            }
        }

        // 3) try tokenized matching: try every n-gram up to 3 words
        String cleaned = normalize(lower); // no diacritics
        String[] tokens = cleaned.split("\\s+");
        for (int len = Math.min(3, tokens.length); len >= 1; len--) {
            for (int i = 0; i + len <= tokens.length; i++) {
                String ng = String.join(" ", Arrays.copyOfRange(tokens, i, i + len));
                if (ALIAS_MAP.containsKey(ng)) return ALIAS_MAP.get(ng);
            }
        }

        // 4) fuzzy match against canonical normalized forms
        String best = null;
        double bestScore = 0.0;
        for (Map.Entry<String, String> e : CANONICAL_NORMALIZED.entrySet()) {
            String norm = e.getKey();
            double score = similarity(norm, cleaned);
            if (score > bestScore) {
                bestScore = score;
                best = e.getValue();
            }
        }
        if (bestScore >= FUZZY_THRESHOLD) return best;

        return null;
    }

    /**
     * Normalize a city string for DB query: lowercase, remove accents, remove prefixes like "thành phố", "tỉnh".
     * Example: "Thành Phố Hà Nội" -> "ha noi" (lowercase, no accent)
     */
    public static String normalizeForQuery(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("\\b(thanh pho|thành phố|tp|tp\\.|tinh|tỉnh|tp)\\b", " ");
        s = s.replaceAll("[^a-z0-9\\s]", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    // ---------- Internal helpers ----------

    // Find best match by first direct alias mapping, otherwise fuzzy
    private static String findBestMatch(String candidate) {
        if (candidate == null || candidate.isBlank()) return null;
        String norm = normalize(candidate);
        if (ALIAS_MAP.containsKey(norm)) return ALIAS_MAP.get(norm);
        if (ALIAS_MAP.containsKey(norm.replaceAll("\\s+", ""))) return ALIAS_MAP.get(norm.replaceAll("\\s+", ""));

        // fuzzy against canonical normalized
        String best = null;
        double bestScore = 0.0;
        for (Map.Entry<String, String> e : CANONICAL_NORMALIZED.entrySet()) {
            String cn = e.getKey();
            double score = similarity(cn, norm);
            if (score > bestScore) {
                bestScore = score;
                best = e.getValue();
            }
        }
        if (bestScore >= FUZZY_THRESHOLD) return best;
        return null;
    }

    // normalize: remove diacritics, lowercase, remove non-alphanum except spaces
    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.toLowerCase(Locale.ROOT);
        n = n.replaceAll("[^a-z0-9\\s]", " ");
        n = n.replaceAll("\\s+", " ").trim();
        return n;
    }

    // similarity based on Levenshtein distance -> ratio in [0..1]
    private static double similarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        if (a.equals(b)) return 1.0;
        int dist = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        if (max == 0) return 1.0;
        return 1.0 - (double) dist / (double) max;
    }

    // standard Levenshtein distance
    private static int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int northwest = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                int above = costs[j] + 1;
                int left = costs[j - 1] + 1;
                int diag = northwest + cost;
                northwest = costs[j];
                costs[j] = Math.min(Math.min(above, left), diag);
            }
        }
        return costs[b.length()];
    }
}
