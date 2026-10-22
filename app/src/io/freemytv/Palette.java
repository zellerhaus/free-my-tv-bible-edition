package io.freemytv;

/**
 * A named color palette for the home screen: the text inks and the backgrounds, by day and by night. The photos stay
 * as they are (almost white by day, almost black at night); the palette shows wherever there is no photo (the Verse
 * tab with a calm background, the drawer, the app without a TMDB key). Picked in Home Settings (Prefs.palette).
 */
final class Palette {
    final String key;
    // By day: the text (ink, dim, faint), the background gradient (top, bottom), a soft glow, the neutral glass.
    final int ink, dim, faint, lightTop, lightBottom, glowLight, glassLight;
    // By night: the text (bright, dimDark), the background gradient, the glow, the neutral glass.
    final int bright, dimDark, darkTop, darkBottom, glowDark, glassDark;

    private Palette(String key, int ink, int dim, int faint, int lightTop, int lightBottom, int glowLight, int glassLight,
            int bright, int dimDark, int darkTop, int darkBottom, int glowDark, int glassDark) {
        this.key = key;
        this.ink = ink;
        this.dim = dim;
        this.faint = faint;
        this.lightTop = lightTop;
        this.lightBottom = lightBottom;
        this.glowLight = glowLight;
        this.glassLight = glassLight;
        this.bright = bright;
        this.dimDark = dimDark;
        this.darkTop = darkTop;
        this.darkBottom = darkBottom;
        this.glowDark = glowDark;
        this.glassDark = glassDark;
    }

    static final Palette[] ALL = {
        // Graphite: the original look (Apple's grays).
        new Palette("graphite", 0xFF1D1D1F, 0xFF6E6E73, 0xFF8E8E93, 0xFFF5F5F7, 0xFFE2E2E7, 0x00FFFFFF, 0xFFEBEBF0,
                0xFFF5F5F7, 0xFF98989D, 0xFF1C1C1E, 0xFF0B0B0D, 0x00000000, 0xFF141416),
        // Parchment: warm paper by day, old leather at night.
        new Palette("parchment", 0xFF2B2318, 0xFF7A6A52, 0xFF9C8B70, 0xFFF7F1E5, 0xFFE9DEC7, 0x33F2D9A8, 0xFFEFE7D6,
                0xFFF4ECDD, 0xFFB3A58E, 0xFF231C14, 0xFF0F0B07, 0x26C98F4A, 0xFF1B1510),
        // Desert: Phoenix at golden hour; deep terracotta at night.
        new Palette("desert", 0xFF3A2418, 0xFF8A5E46, 0xFFA8826C, 0xFFFBF0E4, 0xFFF0D5BE, 0x40F3B07A, 0xFFF5E4D3,
                0xFFFBEFE3, 0xFFC69D85, 0xFF2B1A12, 0xFF120A06, 0x2EE0763A, 0xFF1F130D),
        // Midnight: cool blue-gray by day, deep navy at night.
        new Palette("midnight", 0xFF161B2E, 0xFF5B6482, 0xFF7E869F, 0xFFEEF1F8, 0xFFD9DFEC, 0x339FB4E8, 0xFFE6EAF3,
                0xFFEEF1F8, 0xFF9AA3C2, 0xFF0F1428, 0xFF05070F, 0x2E3A5BD9, 0xFF0B0F1E),
        // Sage: soft green by day, forest at night.
        new Palette("sage", 0xFF1E2A1F, 0xFF5F6E5F, 0xFF83907F, 0xFFF1F5EF, 0xFFDAE4D6, 0x33A9C9A2, 0xFFE9EFE5,
                0xFFEEF4EC, 0xFF97A595, 0xFF141C15, 0xFF070B08, 0x2A3F7A49, 0xFF0F160F),
    };

    static Palette of(String key) {
        for (Palette p : ALL) if (p.key.equals(key)) return p;
        return ALL[0];
    }

    /** The palette's name on screen. */
    static String label(String key) {
        switch (key) {
            case "parchment": return Ui.tr("Pergaminho", "Parchment");
            case "desert": return Ui.tr("Deserto", "Desert");
            case "midnight": return Ui.tr("Meia-noite", "Midnight");
            case "sage": return Ui.tr("Sálvia", "Sage");
            default: return Ui.tr("Grafite", "Graphite");
        }
    }
}
