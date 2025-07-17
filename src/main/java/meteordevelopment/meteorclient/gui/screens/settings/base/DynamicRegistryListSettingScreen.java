/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.base;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public abstract class DynamicRegistryListSettingScreen<T> extends CollectionListSettingScreen<RegistryKey<T>> {
    protected final RegistryKey<Registry<T>> registryKey;

    public DynamicRegistryListSettingScreen(GuiTheme theme, String title, Setting<?> setting, Collection<RegistryKey<T>> collection, RegistryKey<Registry<T>> registryKey) {
        super(theme, title, setting, collection, createUniverse(collection, registryKey));

        this.registryKey = registryKey;
    }

    private static <T> Iterable<RegistryKey<T>> createUniverse(Collection<RegistryKey<T>> collection, RegistryKey<Registry<T>> registryKey) {
        Set<RegistryKey<T>> set = new ReferenceOpenHashSet<>(collection);

        Optional.ofNullable(MinecraftClient.getInstance().getNetworkHandler())
            .map(networkHandler -> (RegistryWrapper.WrapperLookup) networkHandler.getRegistryManager())
            .orElseGet(BuiltinRegistries::createWrapperLookup)
            .getOptional(registryKey)
            .ifPresent(registry -> registry.streamKeys().forEach(set::add));

        return set;
    }

    @Override
    protected void postWidgets(WTable left, WTable right) {
        if (!left.cells.isEmpty()) {
            left.add(theme.horizontalSeparator()).expandX();
            left.row();
        }

        WHorizontalList manualEntry = left.add(theme.horizontalList()).expandX().widget();
        WTextBox textBox = manualEntry.add(theme.textBox("minecraft:")).expandX().minWidth(120d).widget();
        manualEntry.add(theme.plus()).expandCellX().right().widget().action = () -> {
            String entry = textBox.get().trim();
            try {
                Identifier id = entry.contains(":") ? Identifier.of(entry) : Identifier.ofVanilla(entry);
                addValue(RegistryKey.of(registryKey, id));
            } catch (InvalidIdentifierException ignored) {}
        };
    }
}
