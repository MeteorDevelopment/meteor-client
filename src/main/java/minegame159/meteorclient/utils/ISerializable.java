package minegame159.meteorclient.utils;

import net.minecraft.nbt.CompoundTag;

public interface ISerializable<T> {
    public CompoundTag toTag();

    public T fromTag(CompoundTag tag);
}
