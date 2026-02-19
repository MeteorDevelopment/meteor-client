/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements.keyboard;

import meteordevelopment.meteorclient.systems.hud.elements.keyboard.KeyboardHud.Key;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.client.option.KeyBinding;

final class LayoutContext {
    final double keyUnit;
    final double keyGap;
    final double step;
    final double functionRowGap;

    LayoutContext(double keyUnit, double keyGap, double functionRowGap) {
        this.keyUnit = keyUnit;
        this.keyGap = keyGap;
        this.step = keyUnit + keyGap;
        this.functionRowGap = functionRowGap;
    }

    /**
     * Converts units to pixels in the X direction (horizontal).
     *
     * @param units number of key units (e.g., 1.0 = one key width + gap)
     * @return pixel position
     */
    double ux(double units) {
        return units * step;
    }

    /**
     * Converts rows to pixels in the Y direction (vertical).
     * Does not include function row gap - use for simple layouts.
     *
     * @param rows number of key rows
     * @return pixel position
     */
    double y(double rows) {
        return rows * step;
    }

    /**
     * Converts rows to pixels in the Y direction (vertical).
     * Automatically includes the function row gap for rows 1+.
     *
     * @param rows number of key rows (e.g., 0 = function row, 1 = number row, etc.)
     * @return pixel position including function row gap
     */
    double uy(double rows) {
        return rows * step + (rows > 0 ? functionRowGap : 0);
    }

    /**
     * Converts a KeyDimensions to pixels, accounting for gaps.
     *
     * @param d the key dimension
     * @return pixel width/height
     */
    double px(KeyDimensions d) {
        return d.toPixels(keyUnit, keyGap);
    }

    // Core key creation methods - Keybind versions

    /**
     * Creates a standard 1u*1u key at the given position.
     */
    Key key(Keybind kb, double x, double y) {
        return new Key(kb, null, x, y, px(KeyDimensions.STANDARD), px(KeyDimensions.STANDARD));
    }

    /**
     * Creates a key with the given width and standard height.
     */
    Key key(Keybind kb, double x, double y, KeyDimensions w) {
        return new Key(kb, null, x, y, px(w), px(KeyDimensions.STANDARD));
    }

    /**
     * Creates a key with the given width and height.
     */
    Key key(Keybind kb, double x, double y, KeyDimensions w, KeyDimensions h) {
        return new Key(kb, null, x, y, px(w), px(h));
    }

    Key keyNamed(Keybind kb, String name, double x, double y, KeyDimensions w) {
        return new Key(kb, name, x, y, px(w), px(KeyDimensions.STANDARD));
    }

    // Core key creation methods - KeyBinding versions

    /**
     * Creates a standard 1u*1u key at the given position.
     */
    Key key(KeyBinding kb, double x, double y) {
        return new Key(kb, null, x, y, px(KeyDimensions.STANDARD), px(KeyDimensions.STANDARD));
    }

    /**
     * Creates a key with the given width and standard height.
     */
    Key key(KeyBinding kb, double x, double y, KeyDimensions w) {
        return new Key(kb, null, x, y, px(w), px(KeyDimensions.STANDARD));
    }

    /**
     * Creates a named key with the given width and standard height.
     */
    Key key(KeyBinding kb, String name, double x, double y) {
        return new Key(kb, name, x, y, px(KeyDimensions.STANDARD), px(KeyDimensions.STANDARD));
    }
}
