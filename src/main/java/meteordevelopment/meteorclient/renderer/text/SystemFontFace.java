package meteordevelopment.meteorclient.renderer.text;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@NullMarked
public final class SystemFontFace extends FontFace {
    private final Path path;

    public SystemFontFace(FontInfo info, Path path) {
        super(info);

        this.path = path;
    }

    @Override
    public ReadableByteChannel byteChannelForRead() throws IOException {
        return FileChannel.open(this.path, StandardOpenOption.READ);
    }

    @Override
    public String toString() {
        return super.toString() + " (" + path.toString() + ")";
    }
}
