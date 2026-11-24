package com.gathin.tvhome;

/** accent_text_color (Zaytoon fork): "#RRGGBB" as an opaque colour, or 0 (keep the launcher's own) for anything else. */
final class AccentText {
    private AccentText() {
    }

    static int parse(String hex) {
        String h = hex == null ? "" : hex.trim();
        if (!h.matches("#[0-9A-Fa-f]{6}")) return 0;
        return 0xFF000000 | Integer.parseInt(h.substring(1), 16);
    }
}
