package meteordevelopment.meteorclient.utils.render.color;

/**
 * Ember's colour theme. Every UI colour, not just the accent, is derived from the selected
 * theme's hue, so switching themes recolours backgrounds, headers, controls and text too.
 */
public final class EmberPalette {
    public static final String[] NAMES = {"Purple", "Pink", "Blue", "Red", "Emerald", "Amber", "Aqua", "Rainbow"};
    public static final int RAINBOW = 7;

    private static final int[] RGB = {0xA78BE0, 0xE96EAA, 0x6898E8, 0xDE5656, 0x56CE86, 0xE7A450, 0x58C9D0, 0xFFFFFF};

    private static final long RAINBOW_PERIOD_MS = 6000;

    private static int selected = 0;
    private static int cachedRgb = -1;

    private static Color accent, panel, header, divider, rowActive, control, controlHover, track,
        toggleOff, dotOff, search, textBright, textDim, textFaint;

    private EmberPalette() {
    }

    public static int selected() {
        return selected;
    }

    public static void select(int index) {
        if (index >= 0 && index < NAMES.length) selected = index;
    }

    public static Color swatch(int index) {
        if (index == RAINBOW) return Color.fromHsv(rainbowHue() * 360, 0.6, 0.95);
        int rgb = RGB[index];
        return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255);
    }

    public static Color accent() { refresh(); return accent; }
    public static Color panel() { refresh(); return panel; }
    public static Color header() { refresh(); return header; }
    public static Color divider() { refresh(); return divider; }
    public static Color rowActive() { refresh(); return rowActive; }
    public static Color control() { refresh(); return control; }
    public static Color controlHover() { refresh(); return controlHover; }
    public static Color track() { refresh(); return track; }
    public static Color toggleOff() { refresh(); return toggleOff; }
    public static Color dotOff() { refresh(); return dotOff; }
    public static Color search() { refresh(); return search; }
    public static Color textBright() { refresh(); return textBright; }
    public static Color textDim() { refresh(); return textDim; }
    public static Color textFaint() { refresh(); return textFaint; }

    /** Time based, so rainbow keeps animating on the HUD while no screen is open. */
    private static double rainbowHue() {
        return (System.currentTimeMillis() % RAINBOW_PERIOD_MS) / (double) RAINBOW_PERIOD_MS;
    }

    private static void refresh() {
        int rgb;
        if (selected == RAINBOW) {
            Color c = Color.fromHsv(rainbowHue() * 360, 0.55, 0.95);
            rgb = (c.r << 16) | (c.g << 8) | c.b;
        } else {
            rgb = RGB[selected];
        }

        if (rgb == cachedRgb) return;
        cachedRgb = rgb;

        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        accent = new Color(r, g, b, 255);

        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
        double hue = hsb[0] * 360;

        // Tuned so Purple reproduces the original hand-picked colours; saturation is capped so
        // vivid themes stay dark and readable instead of turning the panels neon.
        double bgSat = Math.min(0.55, hsb[1] * 1.12);
        double textSat = Math.min(0.25, hsb[1] * 0.45);

        panel = tone(hue, bgSat, 0.125, 242);
        header = tone(hue, bgSat * 0.95, 0.17, 250);
        divider = tone(hue, bgSat * 0.9, 0.26, 255);
        rowActive = tone(hue, bgSat * 0.9, 0.33, 255);
        control = tone(hue, bgSat, 0.20, 255);
        controlHover = tone(hue, bgSat * 0.95, 0.27, 255);
        track = tone(hue, bgSat * 0.9, 0.27, 255);
        toggleOff = tone(hue, bgSat * 0.85, 0.31, 255);
        dotOff = tone(hue, bgSat * 0.75, 0.33, 255);
        search = tone(hue, bgSat, 0.145, 242);
        textBright = tone(hue, textSat * 0.25, 0.97, 255);
        textDim = tone(hue, textSat, 0.61, 255);
        textFaint = tone(hue, textSat * 1.2, 0.46, 255);
    }

    private static Color tone(double hue, double saturation, double value, int alpha) {
        Color c = Color.fromHsv(hue, saturation, value);
        return new Color(c.r, c.g, c.b, alpha);
    }
}
