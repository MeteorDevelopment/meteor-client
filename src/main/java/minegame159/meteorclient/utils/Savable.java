package minegame159.meteorclient.utils;

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
            file.getParentFile().mkdirs();
            NbtIo.write(toTag(), file);
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
