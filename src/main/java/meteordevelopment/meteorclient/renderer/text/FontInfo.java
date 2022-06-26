package meteordevelopment.meteorclient.renderer.text;

public record FontInfo(String family, FontFace.Type type) {
    public boolean equals(FontInfo info) {
        if (this == info) return true;
        if (info == null || family == null || type == null) return false;
        return family.equals(info.family) && type == info.type;
    }
}
