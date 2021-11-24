/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.settings.*;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.widgets.*;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.*;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DefaultSettingsWidgetFactory implements SettingsWidgetFactory {
    protected interface Factory {
        void create(WTable table, Setting<?> setting);
    }

    private final GuiTheme theme;
    private final Map<Class<?>, Factory> factories = new HashMap<>();

    public DefaultSettingsWidgetFactory(GuiTheme theme) {
        this.theme = theme;

        factories.put(BoolSetting.class, (table, setting) -> boolW(table, (BoolSetting) setting));
        factories.put(IntSetting.class, (table, setting) -> intW(table, (IntSetting) setting));
        factories.put(DoubleSetting.class, (table, setting) -> doubleW(table, (DoubleSetting) setting));
        factories.put(StringSetting.class, (table, setting) -> stringW(table, (StringSetting) setting));
        factories.put(EnumSetting.class, (table, setting) -> enumW(table, (EnumSetting<? extends Enum<?>>) setting));
        factories.put(ProvidedStringSetting.class, (table, setting) -> providedStringW(table, (ProvidedStringSetting) setting));
        factories.put(GenericSetting.class, (table, setting) -> genericW(table, (GenericSetting<?>) setting));
        factories.put(ColorSetting.class, (table, setting) -> colorW(table, (ColorSetting) setting));
        factories.put(KeybindSetting.class, (table, setting) -> keybindW(table, (KeybindSetting) setting));
        factories.put(BlockSetting.class, (table, setting) -> blockW(table, (BlockSetting) setting));
        factories.put(BlockListSetting.class, (table, setting) -> blockListW(table, (BlockListSetting) setting));
        factories.put(ItemSetting.class, (table, setting) -> itemW(table, (ItemSetting) setting));
        factories.put(ItemListSetting.class, (table, setting) -> itemListW(table, (ItemListSetting) setting));
        factories.put(EntityTypeListSetting.class, (table, setting) -> entityTypeListW(table, (EntityTypeListSetting) setting));
        factories.put(EnchantmentListSetting.class, (table, setting) -> enchantmentListW(table, (EnchantmentListSetting) setting));
        factories.put(ModuleListSetting.class, (table, setting) -> moduleListW(table, (ModuleListSetting) setting));
        factories.put(PacketListSetting.class, (table, setting) -> packetListW(table, (PacketListSetting) setting));
        factories.put(ParticleTypeListSetting.class, (table, setting) -> particleTypeListW(table, (ParticleTypeListSetting) setting));
        factories.put(SoundEventListSetting.class, (table, setting) -> soundEventListW(table, (SoundEventListSetting) setting));
        factories.put(StatusEffectAmplifierMapSetting.class, (table, setting) -> statusEffectAmplifierMapW(table, (StatusEffectAmplifierMapSetting) setting));
        factories.put(StatusEffectListSetting.class, (table, setting) -> statusEffectListW(table, (StatusEffectListSetting) setting));
        factories.put(StorageBlockListSetting.class, (table, setting) -> storageBlockListW(table, (StorageBlockListSetting) setting));
        factories.put(BlockDataSetting.class, (table, setting) -> blockDataW(table, (BlockDataSetting<?>) setting));
        factories.put(PotionSetting.class, (table, setting) -> potionW(table, (PotionSetting) setting));
        factories.put(StringListSetting.class, (table, setting) -> stringListW(table, (StringListSetting) setting));
        factories.put(BlockPosSetting.class, (table, setting) -> blockPosW(table, (BlockPosSetting) setting));
    }

    @Override
    public WWidget create(GuiTheme theme, Settings settings, String filter) {
        WVerticalList list = theme.verticalList();

        List<RemoveInfo> removeInfoList = new ArrayList<>();

        // Add all settings
        for (SettingGroup group : settings.groups) {
            group(list, group, filter, removeInfoList);
        }

        // Calculate width and set it as minimum width
        list.calculateSize();
        list.minWidth = list.width;

        // Remove hidden settings
        for (RemoveInfo removeInfo : removeInfoList) removeInfo.remove(list);

        return list;
    }

    // If a different theme uses has different heights of widgets this can method can be overwritten to account for it in the setting titles
    protected double settingTitleTopMargin() {
        return 6;
    }

    private void group(WVerticalList list, SettingGroup group, String filter, List<RemoveInfo> removeInfoList) {
        WSection section = list.add(theme.section(group.name, group.sectionExpanded)).expandX().widget();
        section.action = () -> group.sectionExpanded = section.isExpanded();

        WTable table = section.add(theme.table()).expandX().widget();

        RemoveInfo removeInfo = null;

        for (Setting<?> setting : group) {
            if (!StringUtils.containsIgnoreCase(setting.title, filter)) continue;

            boolean visible = setting.isVisible();
            setting.lastWasVisible = visible;
            if (!visible) {
                if (removeInfo == null) removeInfo = new RemoveInfo(section, table);
                removeInfo.markRowForRemoval();
            }

            table.add(theme.label(setting.title)).top().marginTop(settingTitleTopMargin()).widget().tooltip = setting.description;

            Factory factory = factories.get(setting.getClass());
            if (factory != null) factory.create(table, setting);

            table.row();
        }

        if (removeInfo != null) removeInfoList.add(removeInfo);
    }

    private static class RemoveInfo {
        private final WSection section;
        private final WTable table;
        private final IntList rowIds = new IntArrayList();

        public RemoveInfo(WSection section, WTable table) {
            this.section = section;
            this.table = table;
        }

        public void markRowForRemoval() {
            rowIds.add(table.rowI());
        }

        public void remove(WVerticalList list) {
            for (int i = 0; i < rowIds.size(); i++) {
                table.removeRow(rowIds.getInt(i) - i);
            }

            if (table.cells.isEmpty()) list.cells.removeIf(cell -> cell.widget() == section);
        }
    }

    // Settings

    private void boolW(WTable table, BoolSetting setting) {
        WCheckbox checkbox = table.add(theme.checkbox(setting.get())).expandCellX().widget();
        checkbox.action = () -> setting.set(checkbox.checked);

        reset(table, setting, () -> checkbox.checked = setting.get());
    }

    private void intW(WTable table, IntSetting setting) {
        WIntEdit edit = table.add(theme.intEdit(setting.get(), setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.noSlider)).expandX().widget();

        edit.actionOnRelease = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        reset(table, setting, () -> edit.set(setting.get()));
    }

    private void doubleW(WTable table, DoubleSetting setting) {
        WDoubleEdit edit = theme.doubleEdit(setting.get(), setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.decimalPlaces, setting.noSlider);
        table.add(edit).expandX();

        Runnable action = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        if (setting.onSliderRelease) edit.actionOnRelease = action;
        else edit.action = action;

        reset(table, setting, () -> edit.set(setting.get()));
    }

    private void stringW(WTable table, StringSetting setting) {
        WTextBox textBox = table.add(theme.textBox(setting.get())).expandX().widget();
        textBox.action = () -> setting.set(textBox.get());

        reset(table, setting, () -> textBox.set(setting.get()));
    }

    private void stringListW(WTable table, StringListSetting setting) {
        WTable wtable = table.add(theme.table()).widget();
        StringListSetting.fillTable(theme, wtable, setting);
    }

    private <T extends Enum<?>> void enumW(WTable table, EnumSetting<T> setting) {
        WDropdown<T> dropdown = table.add(theme.dropdown(setting.get())).expandCellX().widget();
        dropdown.action = () -> setting.set(dropdown.get());

        reset(table, setting, () -> dropdown.set(setting.get()));
    }

    private void providedStringW(WTable table, ProvidedStringSetting setting) {
        WDropdown<String> dropdown = table.add(theme.dropdown(setting.supplier.get(), setting.get())).expandCellX().widget();
        dropdown.action = () -> setting.set(dropdown.get());

        reset(table, setting, () -> dropdown.set(setting.get()));
    }

    private void genericW(WTable table, GenericSetting<?> setting) {
        WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
        edit.action = () -> mc.setScreen(setting.get().createScreen(theme));

        reset(table, setting, null);
    }

    private void colorW(WTable table, ColorSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();

        WQuad quad = list.add(theme.quad(setting.get())).widget();

        WButton edit = list.add(theme.button(GuiRenderer.EDIT)).widget();
        edit.action = () -> mc.setScreen(new ColorSettingScreen(theme, setting));

        reset(table, setting, () -> quad.color = setting.get());
    }

    private void keybindW(WTable table, KeybindSetting setting) {
        WKeybind keybind = table.add(theme.keybind(setting.get(), setting.getDefaultValue())).expandX().widget();
        keybind.action = setting::onChanged;
        setting.widget = keybind;
    }

    private void blockW(WTable table, BlockSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();

        WItem item = list.add(theme.item(setting.get().asItem().getDefaultStack())).widget();

        WButton select = list.add(theme.button("Select")).widget();
        select.action = () -> {
            BlockSettingScreen screen = new BlockSettingScreen(theme, setting);
            screen.onClosed(() -> item.set(setting.get().asItem().getDefaultStack()));

            mc.setScreen(screen);
        };

        reset(table, setting, () -> item.set(setting.get().asItem().getDefaultStack()));
    }

    private void blockPosW(WTable table, BlockPosSetting setting) {
        WBlockPosEdit edit = table.add(theme.blockPosEdit(setting.get())).expandX().widget();

        edit.actionOnRelease = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        reset(table, setting, () -> edit.set(setting.get()));
    }

    private void blockListW(WTable table, BlockListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new BlockListSettingScreen(theme, setting)));
    }

    private void itemW(WTable table, ItemSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();

        WItem item = list.add(theme.item(setting.get().asItem().getDefaultStack())).widget();

        WButton select = list.add(theme.button("Select")).widget();
        select.action = () -> {
            ItemSettingScreen screen = new ItemSettingScreen(theme, setting);
            screen.onClosed(() -> item.set(setting.get().getDefaultStack()));

            mc.setScreen(screen);
        };

        reset(table, setting, () -> item.set(setting.get().getDefaultStack()));
    }

    private void itemListW(WTable table, ItemListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new ItemListSettingScreen(theme, setting)));
    }

    private void entityTypeListW(WTable table, EntityTypeListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new EntityTypeListSettingScreen(theme, setting)));
    }

    private void enchantmentListW(WTable table, EnchantmentListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new EnchantmentListSettingScreen(theme, setting)));
    }

    private void moduleListW(WTable table, ModuleListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new ModuleListSettingScreen(theme, setting)));
    }

    private void packetListW(WTable table, PacketListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new PacketBoolSettingScreen(theme, setting)));
    }

    private void particleTypeListW(WTable table, ParticleTypeListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new ParticleTypeListSettingScreen(theme, setting)));
    }

    private void soundEventListW(WTable table, SoundEventListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new SoundEventListSettingScreen(theme, setting)));
    }

    private void statusEffectAmplifierMapW(WTable table, StatusEffectAmplifierMapSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new StatusEffectAmplifierMapSettingScreen(theme, setting)));
    }

    private void statusEffectListW(WTable table, StatusEffectListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new StatusEffectListSettingScreen(theme, setting)));
    }

    private void storageBlockListW(WTable table, StorageBlockListSetting setting) {
        selectW(table, setting, () -> mc.setScreen(new StorageBlockListSettingScreen(theme, setting)));
    }

    private void blockDataW(WTable table, BlockDataSetting<?> setting) {
        WButton button = table.add(theme.button(GuiRenderer.EDIT)).expandCellX().widget();
        button.action = () -> mc.setScreen(new BlockDataSettingScreen(theme, setting));

        reset(table, setting, null);
    }

    private void potionW(WTable table, PotionSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();
        WItemWithLabel item = list.add(theme.itemWithLabel(setting.get().potion, setting.get().potion.getName().getString())).widget();

        WButton button = list.add(theme.button("Select")).expandCellX().widget();
        button.action = () -> {
            WidgetScreen screen = new PotionSettingScreen(theme, setting);
            screen.onClosed(() -> item.set(setting.get().potion));

            mc.setScreen(screen);
        };

        reset(list, setting, () -> item.set(setting.get().potion));
    }

    // Other

    private void selectW(WContainer c, Setting<?> setting, Runnable action) {
        WButton button = c.add(theme.button("Select")).expandCellX().widget();
        button.action = action;

        reset(c, setting, null);
    }

    private void reset(WContainer c, Setting<?> setting, Runnable action) {
        WButton reset = c.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            if (action != null) action.run();
        };
    }
}
