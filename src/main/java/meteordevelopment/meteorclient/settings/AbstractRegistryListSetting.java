/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractRegistryListSetting<C extends Collection<V>, V> extends Setting<C> {
    public final Predicate<V> filter;
    public final Registry<V> registry;

    protected AbstractRegistryListSetting(String name, String description, C defaultValue, Consumer<C> onChanged, Consumer<Setting<C>> onModuleActivated, IVisible visible, Predicate<V> filter, Registry<V> registry) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);
        this.filter = filter;
        this.registry = registry;
    }

    protected abstract C transferCollection(Collection<V> from);

    @Override
    public final void resetImpl() {
        value = transferCollection(defaultValue);
    }

    @Override
    protected final C parseImpl(String str) {
        String[] values = StringUtils.split(str, ',');
        List<V> items = new ArrayList<>(values.length);

        try {
            for (String string : values) {
                V value = parseId(this.registry, string);
                if (value != null && filter.test(value)) items.add(value);
            }
        } catch (Exception ignored) {}

        return transferCollection(items);
    }

    @Override
    protected boolean isValueValid(C value) {
        return true;
    }

    @Override
    public final Iterable<Identifier> getIdentifierSuggestions() {
        return this.registry.getIds();
    }

    @Override
    protected NbtCompound save(NbtCompound tag) {
        NbtList valueTag = new NbtList();
        for (V value : get()) {
            valueTag.add(NbtString.of(this.registry.getId(value).toString()));
        }
        tag.put("value", valueTag);

        return tag;
    }

    @Override
    protected C load(NbtCompound tag) {
        get().clear();

        NbtList valueTag = tag.getListOrEmpty("value");
        for (NbtElement tagI : valueTag) {
            V value = this.registry.get(Identifier.of(tagI.asString().orElse("")));
            if (value == null) continue;

            if (filter.test(value)) get().add(value);
        }

        return get();
    }

}
