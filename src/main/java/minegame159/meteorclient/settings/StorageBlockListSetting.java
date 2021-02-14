/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.screens.settings.StorageBlockListSettingScreen;
import minegame159.meteorclient.gui.widgets.WButton;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StorageBlockListSetting extends Setting<List<BlockEntityType<?>>> {
    public static final BlockEntityType<?>[] STORAGE_BLOCKS = { BlockEntityType.FURNACE, BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST, BlockEntityType.ENDER_CHEST, BlockEntityType.DISPENSER, BlockEntityType.DROPPER, BlockEntityType.HOPPER, BlockEntityType.SHULKER_BOX, BlockEntityType.BARREL, BlockEntityType.SMOKER, BlockEntityType.BLAST_FURNACE };
    public static final String[] STORAGE_BLOCK_NAMES = { "Furnace", "Chest", "Trapped Chest", "Ender Chest", "Dispenser", "Dropper", "Hopper", "Shulker Box", "Barrel", "Smoker", "Blast Furnace" };

    public StorageBlockListSetting(String name, String description, List<BlockEntityType<?>> defaultValue, Consumer<List<BlockEntityType<?>>> onChanged, Consumer<Setting<List<BlockEntityType<?>>>> onModuleActivated) {
        super(name, description, defaultValue, onChanged, onModuleActivated);

        value = new ArrayList<>(defaultValue);

        widget = new WButton("Select");
        ((WButton) widget).action = () -> MinecraftClient.getInstance().openScreen(new StorageBlockListSettingScreen(this));
    }

    @Override
    public void reset(boolean callbacks) {
        value = new ArrayList<>(defaultValue);
        if (callbacks) {
            resetWidget();
            changed();
        }
    }

    @Override
    protected List<BlockEntityType<?>> parseImpl(String str) {
        String[] values = str.split(",");
        List<BlockEntityType<?>> blocks = new ArrayList<>(1);

        try {
            for (String value : values) {
                String val = value.trim();
                Identifier id;
                if (val.contains(":")) id = new Identifier(val);
                else id = new Identifier("minecraft", val);
                if (Registry.BLOCK_ENTITY_TYPE.containsId(id)) blocks.add(Registry.BLOCK_ENTITY_TYPE.get(id));
            }
        } catch (Exception ignored) {}

        return blocks;
    }

    @Override
    public void resetWidget() {

    }

    @Override
    protected boolean isValueValid(List<BlockEntityType<?>> value) {
        return true;
    }

    @Override
    protected String generateUsage() {
        return "(highlight)storage block id(default)(chest, minecraft:ender_chest, etc)";
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = saveGeneral();

        ListTag valueTag = new ListTag();
        for (BlockEntityType<?> type : get()) {
            Identifier id = Registry.BLOCK_ENTITY_TYPE.getId(type);
            if (id != null) valueTag.add(StringTag.of(id.toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    public List<BlockEntityType<?>> fromTag(CompoundTag tag) {
        get().clear();

        ListTag valueTag = tag.getList("value", 8);
        for (Tag tagI : valueTag) {
            BlockEntityType<?> type = Registry.BLOCK_ENTITY_TYPE.get(new Identifier(tagI.asString()));
            if (type != null) get().add(type);
        }

        changed();
        return get();
    }

    public static class Builder {
        private String name = "undefined", description = "";
        private List<BlockEntityType<?>> defaultValue;
        private Consumer<List<BlockEntityType<?>>> onChanged;
        private Consumer<Setting<List<BlockEntityType<?>>>> onModuleActivated;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(List<BlockEntityType<?>> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder onChanged(Consumer<List<BlockEntityType<?>>> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onModuleActivated(Consumer<Setting<List<BlockEntityType<?>>>> onModuleActivated) {
            this.onModuleActivated = onModuleActivated;
            return this;
        }

        public StorageBlockListSetting build() {
            return new StorageBlockListSetting(name, description, defaultValue, onChanged, onModuleActivated);
        }
    }
}
