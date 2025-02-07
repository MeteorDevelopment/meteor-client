/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

public class BlockPosSetting extends Setting<BlockPos> {
    public BlockPosSetting(String name, String description, BlockPos defaultValue, Consumer<BlockPos> onChanged, Consumer<Setting<BlockPos>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        tag.putIntArray("value", new int[] {value.getX(), value.getY(), value.getZ()});

        return tag;
    }

    @Override
    protected BlockPos load(NbtCompound tag) {
        int[] value = tag.getIntArray("value");
        set(new BlockPos(value[0], value[1], value[2]));

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, BlockPos, BlockPosSetting> {
        public Builder() {
            super(new BlockPos(0, 0, 0));
        }

        @Override
        public BlockPosSetting build() {
            return new BlockPosSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
