/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockListSetting extends AbstractRegistryListSetting<Set<Block>, Block> {
    public BlockListSetting(String name, String description, Set<Block> defaultValue, Consumer<Set<Block>> onChanged, Consumer<Setting<Set<Block>>> onModuleActivated, Predicate<Block> filter, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible, filter, Registries.BLOCK);
    }

    @Override
    protected Set<Block> transferCollection(Collection<Block> from) {
        return new ReferenceOpenHashSet<>(from);
    }

    public static class Builder extends SettingBuilder<Builder, Set<Block>, BlockListSetting> {
        private Predicate<Block> filter = Predicates.alwaysTrue();

        public Builder() {
            super(Collections.emptySet());
        }

        public Builder defaultValue(Block... defaults) {
            return defaultValue(defaults != null ? ReferenceOpenHashSet.of(defaults) : new ReferenceOpenHashSet<>());
        }

        public Builder filter(Predicate<Block> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public BlockListSetting build() {
            return new BlockListSetting(name, description, defaultValue, onChanged, onModuleActivated, filter, visible);
        }
    }
}
