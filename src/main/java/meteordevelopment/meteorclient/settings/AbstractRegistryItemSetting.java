/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.common.base.Predicates;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractRegistryItemSetting<V> extends Setting<V> {
    public final Predicate<V> filter;
    public final Registry<V> registry;

    protected AbstractRegistryItemSetting(String name, String description, V defaultValue, Consumer<V> onChanged, Consumer<Setting<V>> onModuleActivated, IVisible visible, Predicate<V> filter, Registry<V> registry) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
        this.registry = registry;
    }

    @Override
    protected final V parseImpl(String str) {
        return parseId(registry, str);
    }

    @Override
    protected final boolean isValueValid(V value) {
        return value != null && filter.test(value);
    }

    @Override
    public final Iterable<Identifier> getIdentifierSuggestions() {
        return registry.getIds();
    }

    @Override
    protected final NbtCompound save(NbtCompound tag) {
        tag.putString("value", registry.getId(get()).toString());

        return tag;
    }

    @Override
    protected final V load(NbtCompound tag) {
        V value = registry.get(Identifier.of(tag.getString("value", "")));

        if (!isValueValid(value)) {
            resetImpl();
        } else {
            this.value = value;
        }

        return get();
    }

    @SuppressWarnings("unchecked")
    protected static abstract class AbstractBuilder<B extends AbstractBuilder<B, V, S>, V, S extends AbstractRegistryItemSetting<V>> extends SettingBuilder<B, V, S> {
        protected Predicate<V> filter = Predicates.alwaysTrue();

        protected AbstractBuilder() {
            super(null);
        }

        public B filter(Predicate<V> filter) {
            this.filter = filter;
            return (B) this;
        }
    }

}

