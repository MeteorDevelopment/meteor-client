/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements.keyboard;

enum KeyDimensions {
    // Standard sizes
    UNIT_1U(1.0),
    UNIT_1_25U(1.25),
    UNIT_1_5U(1.5),
    UNIT_1_75U(1.75),
    UNIT_2U(2.0),
    UNIT_2_25U(2.25),
    UNIT_2_75U(2.75),
    UNIT_6_25U(6.25);

    public final double units;

    KeyDimensions(double units) {
        this.units = units;
    }

    /**
     * Converts this dimension to pixels, accounting for gaps.
     * Multi-unit keys span gaps: 2u key = 2*baseUnit + 1*gap
     */
    public double toPixels(double baseUnit, double gap) {
        return units * baseUnit + (units - 1.0) * gap;
    }

    public double toPixels(double baseUnit) {
        return units * baseUnit;
    }

    // Aliases for common keys
    public static final KeyDimensions STANDARD = UNIT_1U;
    public static final KeyDimensions TAB = UNIT_1_5U;
    public static final KeyDimensions CAPS_LOCK = UNIT_1_75U;
    public static final KeyDimensions ENTER_ANSI = UNIT_2_25U;
    public static final KeyDimensions LEFT_SHIFT_ANSI = UNIT_2_25U;
    public static final KeyDimensions RIGHT_SHIFT = UNIT_2_75U;
    public static final KeyDimensions BACKSPACE = UNIT_2U;
    public static final KeyDimensions LEFT_SHIFT_ISO = UNIT_1_25U;
    public static final KeyDimensions ENTER_ISO_WIDTH = UNIT_1_25U;
    public static final KeyDimensions ENTER_ISO_HEIGHT = UNIT_2U;
    public static final KeyDimensions CTRL = UNIT_1_25U;
    public static final KeyDimensions ALT = UNIT_1_25U;
    public static final KeyDimensions GUI = UNIT_1_25U;
    public static final KeyDimensions MENU = UNIT_1_25U;
    public static final KeyDimensions SPACEBAR = UNIT_6_25U;
}
