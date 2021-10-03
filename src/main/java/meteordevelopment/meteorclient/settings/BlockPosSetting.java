/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.Consumer;

public class BlockPosSetting extends Setting<BlockPos> {
    public BlockPosSetting(String name, String description, BlockPos defaultValue, Consumer<BlockPos> onChanged, Consumer<Setting<BlockPos>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
    }

    @Override
    protected BlockPos parseImpl(String str) {
        List<String> values = List.of(str.split(","));
        if (values.size() != 3) return null;

        BlockPos bp = null;
        try {
            bp = new BlockPos(Integer.parseInt(values.get(0)), Integer.parseInt(values.get(1)), Integer.parseInt(values.get(2)));
        }
        catch (NumberFormatException ignored) {}
        return bp;
    }

    @Override
    protected boolean isValueValid(BlockPos value) {
        return true;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        NbtIntArray array = new NbtIntArray(new int[3]);
        array.set(0, NbtInt.of(value.getX()));
        array.set(1, NbtInt.of(value.getY()));
        array.set(2, NbtInt.of(value.getZ()));

        tag.put("value", array);
        return tag;
    }

    @Override
    public BlockPos fromTag(NbtCompound tag) {
        NbtIntArray value = new NbtIntArray(tag.getIntArray("value"));
        set(new BlockPos(value.get(0).intValue(), value.get(1).intValue(), value.get(2).intValue()));
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private BlockPos defaultValue = new BlockPos(0, 0, 0);
        private Consumer<BlockPos> onChanged;
        private Consumer<Setting<BlockPos>> onModuleActivated;
        private IVisible visible;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(BlockPos defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<BlockPos> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<BlockPos>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder visible(IVisible visible) {
            this.visible = visible;
            return this;
        }

        public BlockPosSetting build() {
            return new BlockPosSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
