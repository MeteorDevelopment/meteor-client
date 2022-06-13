package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.utils.render.FontUtils;

import java.io.InputStream;
import java.nio.file.Path;

public record FontFace(FontInfo info, Path path) {
    public InputStream asStream() {
        InputStream in = FontUtils.stream(path.toFile());
        if (in == null) {
            throw new RuntimeException("Font " + this + " couldn't be loaded");
        }

        return in;
    }

    @Override
    public String toString() {
        return info().family() + " " + info().type();
    }

    public boolean equals(FontFace fontFace) {
        if (fontFace == this) return true;
        if (fontFace == null) return false;
        return info.equals(fontFace.info);
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
