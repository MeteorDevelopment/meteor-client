/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
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
        NbtCompound tag = saveGeneral();
        tag.putIntArray("value", new int[] { value.getX(), value.getY(), value.getZ() });
        return tag;
    }

    @Override
    public BlockPos fromTag(NbtCompound tag) {
        int[] value = tag.getIntArray("value");
        set(new BlockPos(value[0], value[1], value[2]));

        changed();
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
