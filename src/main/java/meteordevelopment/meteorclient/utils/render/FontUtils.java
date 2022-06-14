package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.files.StreamUtils;
import net.minecraft.util.Util;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FontUtils {
    public static FontInfo getFontInfo(File file) {
        InputStream stream = stream(file);
        byte[] bytes = Utils.readBytes(stream);
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(fontInfo, buffer);
        ByteBuffer nameBuffer = STBTruetype.stbtt_GetFontNameString(fontInfo, STBTruetype.STBTT_PLATFORM_ID_MICROSOFT, STBTruetype.STBTT_MS_EID_UNICODE_BMP, STBTruetype.STBTT_MS_LANG_ENGLISH, 1);
        ByteBuffer typeBuffer = STBTruetype.stbtt_GetFontNameString(fontInfo, STBTruetype.STBTT_PLATFORM_ID_MICROSOFT, STBTruetype.STBTT_MS_EID_UNICODE_BMP, STBTruetype.STBTT_MS_LANG_ENGLISH, 2);
        if (typeBuffer == null || nameBuffer == null) {
            return null;
        }

        return new FontInfo(
            StandardCharsets.UTF_16.decode(nameBuffer).toString(),
            FontFace.Type.fromString(StandardCharsets.UTF_16.decode(typeBuffer).toString())
        );
    }

    public static Set<String> getSearchPaths() {
        Set<String> paths = new HashSet<>();
        paths.add(System.getProperty("java.home") + "/lib/fonts");

        for (File dir : getUFontDirs()) {
            if (dir.exists()) paths.add(dir.getAbsolutePath());
        }

        for (File dir : getSFontDirs()) {
            if (dir.exists()) paths.add(dir.getAbsolutePath());
        }

        return paths;
    }

    public static List<File> getUFontDirs() {
        return switch (Util.getOperatingSystem()) {
            case WINDOWS -> List.of(new File(System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Windows\\Fonts"));
            case OSX -> List.of(new File(System.getProperty("user.home") + "/Library/Fonts/"));
            default -> List.of(new File(System.getProperty("user.home") + "/.local/share/fonts"), new File(System.getProperty("user.home") + "/.fonts"));
        };
    }

    public static List<File> getSFontDirs() {
        return switch (Util.getOperatingSystem()) {
            case WINDOWS -> List.of(new File(System.getenv("SystemRoot") + "\\Fonts"));
            case OSX -> List.of(new File("/System/Library/Fonts/"));
            default -> List.of(new File("/usr/share/fonts/"));
        };
    }

    public static File getDir(List<File> dirs) {
        for (File dir : dirs) {
            if (dir.exists()) return dir;
        }

        dirs.get(0).mkdirs();
        return dirs.get(0);
    }

    public static void collectFonts(List<FontFamily> fontList, File dir, Consumer<File> consumer) {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((file) -> (file.isFile() && file.getName().endsWith(".ttf") || file.isDirectory()));
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectFonts(fontList, file, consumer);
                continue;
            }

            FontInfo fontInfo = FontUtils.getFontInfo(file);
            if (fontInfo != null) {
                consumer.accept(file);

                FontFamily family = Fonts.getFamily(fontInfo.family());
                if (family == null) {
                    family = new FontFamily(fontInfo.family());
                    fontList.add(family);
                }

                if (family.add(file) == null) {
                    MeteorClient.LOG.warn("Failed to load font: {}", fontInfo);
                }
            }
        }
    }

    public static void copyBuiltin(String name, File target) {
        try {
            File fontFile = new File(MeteorClient.FOLDER, name + ".ttf");
            fontFile.createNewFile();
            InputStream stream = FontUtils.class.getResourceAsStream("/assets/" + MeteorClient.MOD_ID + "/fonts/" + name + ".ttf");
            StreamUtils.copy(stream, fontFile);
            Files.copy(fontFile.toPath(), new File(target, fontFile.getName()).toPath(), REPLACE_EXISTING);
            fontFile.delete();
        }
        catch (Exception e) {
            MeteorClient.LOG.error("Failed to copy builtin font " + name + " to " + target.getAbsolutePath());
            e.printStackTrace();
            if (name.equals(Fonts.DEFAULT_FONT_FAMILY)) {
                throw new RuntimeException("Failed to load default font.");
            }
        }
    }

    public static InputStream stream(File file) {
        try {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
