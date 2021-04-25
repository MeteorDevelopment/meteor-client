/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.utils.misc.IChangeable;
import minegame159.meteorclient.utils.misc.ICopyable;
import minegame159.meteorclient.utils.misc.IGetter;
import minegame159.meteorclient.utils.misc.ISerializable;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class BlockDataSetting<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> extends Setting<Map<Block, T>> {
    public final IGetter<T> defaultData;

    public BlockDataSetting(String name, String description, Map<Block, T> defaultValue, Consumer<Map<Block, T>> onChanged, Consumer<Setting<Map<Block, T>>> onModuleActivated, IGetter<T> defaultData) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        this.defaultData = defaultData;
    }

    @Override
    public void reset(boolean callbacks) {
        value = new HashMap<>(defaultValue);
        if (callbacks) changed();
    }

    @Override
    protected Map<Block, T> parseImpl(String str) {
        return new HashMap<>(0);
    }

    @Override
    protected boolean isValueValid(Map<Block, T> value) {
        return true;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        CompoundTag valueTag = new CompoundTag();
        for (Block block : get().keySet()) {
            valueTag.put(Registry.BLOCK.getId(block).toString(), get().get(block).toTag());
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public Map<Block, T> fromTag(CompoundTag tag) {
        get().clear();

        CompoundTag valueTag = tag.getCompound("value");
        for (String key : valueTag.getKeys()) {
            get().put(Registry.BLOCK.get(new Identifier(key)), defaultData.get().copy().fromTag(valueTag.getCompound(key)));
        }

        return get();
    }

    public static class Builder<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> {
        private String name = "undefined", description = "";
        private Map<Block, T> defaultValue;
        private Consumer<Map<Block, T>> onChanged;
        private Consumer<Setting<Map<Block, T>>> onModuleActivated;
        private IGetter<T> defaultData;

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> defaultValue(Map<Block, T> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> onChanged(Consumer<Map<Block, T>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder<T> onModuleActivated(Consumer<Setting<Map<Block, T>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public Builder<T> defaultData(IGetter<T> defaultData) {
            this.defaultData = defaultData;
            return this;
        }

        public BlockDataSetting<T> build() {
            return new BlockDataSetting<>(name, description, defaultValue, onChanged, onModuleActivated, defaultData);
        }
    }
}
