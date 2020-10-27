package minegame159.meteorclient.utils;

import com.google.common.io.Files;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;

public abstract class Savable<T> implements ISerializable<T> {
    private final File file;

    public Savable(File file) {
        this.file = file;
    }

    public void save(File file) {
        try {
            File tempFile = File.createTempFile("meteor-client", file.getName());
            NbtIo.write(toTag(), tempFile);

            file.getParentFile().mkdirs();
            Files.copy(tempFile, file);
            tempFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void save() {
        save(getFile());
    }

    public boolean load(File file) {
        try {
            if (file.exists()) {
                fromTag(NbtIo.read(file));
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
    public boolean load() {
        return load(getFile());
    }

    public File getFile() {
        return file;
    }
}
