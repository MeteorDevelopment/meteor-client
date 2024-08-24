/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.Pair;

import java.util.*;
import java.util.function.Consumer;

public abstract class DynamicRegistryListSettingScreen<E> extends WindowScreen {
    protected final Setting<?> setting;
    protected final Collection<RegistryKey<E>> collection;
    private final RegistryKey<Registry<E>> registryKey;
    private final Optional<Registry<E>> registry;

    private WTextBox filter;
    private String filterText = "";

    private WTable table;

    public DynamicRegistryListSettingScreen(GuiTheme theme, String title, Setting<?> setting, Collection<RegistryKey<E>> collection, RegistryKey<Registry<E>> registryKey) {
        super(theme, title);

        this.registryKey = registryKey;
        this.registry = Optional.ofNullable(MinecraftClient.getInstance().getNetworkHandler())
            .flatMap(networkHandler -> networkHandler.getRegistryManager().getOptional(registryKey));
        this.setting = setting;
        this.collection = collection;
    }

    @Override
    public void initWidgets() {
        // Filter
        filter = add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            table.clear();
            generateWidgets();
        };

        table = add(theme.table()).expandX().widget();

        generateWidgets();
    }

    private void generateWidgets() {
        // Left (all)
        WTable left = abc(pairs -> registry.ifPresent(registry -> {
            registry.streamEntries()
                .map(RegistryEntry.Reference::getKey)
                .filter(Optional::isPresent)
                .map(Optional::get).forEach(t -> {
                    if (skipValue(t) || collection.contains(t)) return;

                    int words = Utils.searchInWords(getValueName(t), filterText);
                    int diff = Utils.searchLevenshteinDefault(getValueName(t), filterText, false);
                    if (words > 0 || diff <= getValueName(t).length() / 2) pairs.add(new Pair<>(t, -diff));
                });
            }), true, t -> {
                addValue(t);

                RegistryKey<E> v = getAdditionalValue(t);
                if (v != null) addValue(v);
            }
        );

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
            } catch (InvalidIdentifierException e) {}
        };

        table.add(theme.verticalSeparator()).expandWidgetY();

        // Right (selected)
        abc(pairs -> {
            for (RegistryKey<E> value : collection) {
                if (skipValue(value)) continue;

                int words = Utils.searchInWords(getValueName(value), filterText);
                int diff = Utils.searchLevenshteinDefault(getValueName(value), filterText, false);
                if (words > 0 || diff <= getValueName(value).length() / 2) pairs.add(new Pair<>(value, -diff));
            }
        }, false, t -> {
            removeValue(t);

            RegistryKey<E> v = getAdditionalValue(t);
            if (v != null) removeValue(v);
        });
    }

    private void addValue(RegistryKey<E> value) {
        if (!collection.contains(value)) {
            collection.add(value);

            setting.onChanged();
            table.clear();
            generateWidgets();
        }
    }

    private void removeValue(RegistryKey<E> value) {
        if (collection.remove(value)) {
            setting.onChanged();
            table.clear();
            generateWidgets();
        }
    }

    private WTable abc(Consumer<List<Pair<RegistryKey<E>, Integer>>> addValues, boolean isLeft, Consumer<RegistryKey<E>> buttonAction) {
        // Create
        Cell<WTable> cell = this.table.add(theme.table()).top();
        WTable table = cell.widget();

        Consumer<RegistryKey<E>> forEach = t -> {
            if (!includeValue(t)) return;

            table.add(getValueWidget(t));

            WPressable button = table.add(isLeft ? theme.plus() : theme.minus()).expandCellX().right().widget();
            button.action = () -> buttonAction.accept(t);

            table.row();
        };

        // Sort
        List<Pair<RegistryKey<E>, Integer>> values = new ArrayList<>();
        addValues.accept(values);
        if (!filterText.isEmpty()) values.sort(Comparator.comparingInt(value -> -value.getRight()));
        for (Pair<RegistryKey<E>, Integer> pair : values) forEach.accept(pair.getLeft());

        if (!table.cells.isEmpty()) cell.expandX();

        return table;
    }

    protected boolean includeValue(RegistryKey<E> value) {
        return true;
    }

    protected abstract WWidget getValueWidget(RegistryKey<E> value);

    protected abstract String getValueName(RegistryKey<E> value);

    protected boolean skipValue(RegistryKey<E> value) {
        return false;
    }

    protected RegistryKey<E> getAdditionalValue(RegistryKey<E> value) {
        return null;
    }
}
