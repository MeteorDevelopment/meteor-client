/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

public record FontInfo(String family, Type type) {
    @Override
    public String toString() {
        return family + " " + type;
    }

    public boolean equals(FontInfo info) {
        if (this == info) return true;
        if (info == null || family == null || type == null) return false;
        return family.equals(info.family) && type == info.type;
    }

    public enum Type {
        Regular,
        Bold,
        Italic,
        BoldItalic;

        public static Type fromString(String str) {
            return switch (str) {
                case "Bold" -> Bold;
                case "Italic" -> Italic;
                case "Bold Italic", "BoldItalic" -> BoldItalic;
                default -> Regular;
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case Bold -> "Bold";
                case Italic -> "Italic";
                case BoldItalic -> "Bold Italic";
                default -> "Regular";
            };
        }
    }
}
