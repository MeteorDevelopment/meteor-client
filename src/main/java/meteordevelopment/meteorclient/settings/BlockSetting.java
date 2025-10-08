/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockSetting extends AbstractRegistryItemSetting<Block> {
    public BlockSetting(String name, String description, Block defaultValue, Consumer<Block> onChanged, Consumer<Setting<Block>> onModuleActivated, IVisible visible, Predicate<Block> filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, Registries.BLOCK);
    }

    public static class Builder extends AbstractBuilder<Builder, Block, BlockSetting> {
        @Override
        public BlockSetting build() {
            return new BlockSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
    }
}
