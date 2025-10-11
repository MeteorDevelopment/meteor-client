/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import com.google.common.base.Predicates;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class StatusEffectListSetting extends AbstractRegistryListSetting<List<StatusEffect>, StatusEffect> {
    public StatusEffectListSetting(String name, String description, List<StatusEffect> defaultValue, Consumer<List<StatusEffect>> onChanged, Consumer<Setting<List<StatusEffect>>> onModuleActivated, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible, Predicates.alwaysTrue(), Registries.STATUS_EFFECT);
    }

    @Override
    protected List<StatusEffect> transferCollection(Collection<StatusEffect> from) {
        return new ArrayList<>(from);
    }

    public static class Builder extends SettingBuilder<Builder, List<StatusEffect>, StatusEffectListSetting> {
        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(StatusEffect... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        @Override
        public StatusEffectListSetting build() {
            return new StatusEffectListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
        }
    }
}
