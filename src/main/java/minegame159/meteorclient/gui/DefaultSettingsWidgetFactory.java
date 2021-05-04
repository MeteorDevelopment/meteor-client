/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.screens.settings.*;
import minegame159.meteorclient.gui.utils.SettingsWidgetFactory;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.gui.widgets.containers.*;
import minegame159.meteorclient.gui.widgets.input.WDoubleEdit;
import minegame159.meteorclient.gui.widgets.input.WDropdown;
import minegame159.meteorclient.gui.widgets.input.WIntEdit;
import minegame159.meteorclient.gui.widgets.input.WTextBox;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.gui.widgets.pressable.WCheckbox;
import minegame159.meteorclient.settings.*;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static minegame159.meteorclient.utils.Utils.mc;

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
        factories.put(EnumSetting.class, (table, setting) -> enumW(table, (EnumSetting<? extends Enum<?>>) setting));
        factories.put(PotionSetting.class, (table, setting) -> potionW(table, (PotionSetting) setting));
        factories.put(ColorSetting.class, (table, setting) -> colorW(table, (ColorSetting) setting));
        factories.put(StringSetting.class, (table, setting) -> stringW(table, (StringSetting) setting));
        factories.put(ProvidedStringSetting.class, (table, setting) -> providedStringW(table, (ProvidedStringSetting) setting));
        factories.put(BlockSetting.class, (table, setting) -> blockW(table, (BlockSetting) setting));
        factories.put(KeybindSetting.class, (table, setting) -> keybindW(table, (KeybindSetting) setting));
        factories.put(GenericSetting.class, (table, setting) -> genericW(table, (GenericSetting<?>) setting));
        factories.put(BlockListSetting.class, (table, setting) -> blockListW(table, (BlockListSetting) setting));
        factories.put(ItemListSetting.class, (table, setting) -> itemListW(table, (ItemListSetting) setting));
        factories.put(EntityTypeListSetting.class, (table, setting) -> entityTypeListW(table, (EntityTypeListSetting) setting));
        factories.put(EnchListSetting.class, (table, setting) -> enchListW(table, (EnchListSetting) setting));
        factories.put(ModuleListSetting.class, (table, setting) -> moduleListW(table, (ModuleListSetting) setting));
        factories.put(PacketBoolSetting.class, (table, setting) -> packetBoolW(table, (PacketBoolSetting) setting));
        factories.put(ParticleTypeListSetting.class, (table, setting) -> particleEffectListW(table, (ParticleTypeListSetting) setting));
        factories.put(SoundEventListSetting.class, (table, setting) -> soundEventListW(table, (SoundEventListSetting) setting));
        factories.put(StatusEffectSetting.class, (table, setting) -> statusEffectW(table, (StatusEffectSetting) setting));
        factories.put(StorageBlockListSetting.class, (table, setting) -> storageBlockListW(table, (StorageBlockListSetting) setting));
        factories.put(BlockDataSetting.class, (table, setting) -> blockDataSettingW(table, (BlockDataSetting<?>) setting));
    }

    @Override
    public WWidget create(GuiTheme theme, Settings settings, String filter) {
        WVerticalList list = theme.verticalList();

        for (SettingGroup group : settings.groups) {
            group(list, group, filter);
        }

        return list;
    }

    private void group(WVerticalList list, SettingGroup group, String filter) {
        WSection section = list.add(theme.section(group.name, group.sectionExpanded)).expandX().widget();
        section.action = () -> group.sectionExpanded = section.isExpanded();

        WTable table = section.add(theme.table()).expandX().widget();

        for (Setting<?> setting : group) {
            if (!StringUtils.containsIgnoreCase(setting.title, filter)) continue;

            table.add(theme.label(setting.title)).widget().tooltip = setting.description;

            Factory factory = factories.get(setting.getClass());
            if (factory != null) factory.create(table, setting);

            table.row();
        }

        if (table.cells.isEmpty()) list.cells.remove(list.cells.size() - 1);
    }

    // Settings

    private void boolW(WTable table, BoolSetting setting) {
        WCheckbox checkbox = table.add(theme.checkbox(setting.get())).expandCellX().widget();
        checkbox.action = () -> setting.set(checkbox.checked);

        reset(table, setting, () -> checkbox.checked = setting.get());
    }

    private void intW(WTable table, IntSetting setting) {
        WIntEdit edit = table.add(theme.intEdit(setting.get(), setting.getSliderMin(), setting.getSliderMax())).expandX().widget();
        edit.min = setting.min;
        edit.max = setting.max;

        edit.actionOnRelease = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        reset(table, setting, () -> edit.set(setting.get()));
    }

    private void doubleW(WTable table, DoubleSetting setting) {
        WDoubleEdit edit = theme.doubleEdit(setting.get(), setting.getSliderMin(), setting.getSliderMax());
        edit.min = setting.min;
        edit.max = setting.max;
        edit.decimalPlaces = setting.decimalPlaces;

        table.add(edit).expandX();

        Runnable action = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        if (setting.onSliderRelease) edit.actionOnRelease = action;
        else edit.action = action;

        reset(table, setting, () -> edit.set(setting.get()));
    }

    private <T extends Enum<?>> void enumW(WTable table, EnumSetting<T> setting) {
        WDropdown<T> dropdown = table.add(theme.dropdown(setting.get())).expandCellX().widget();
        dropdown.action = () -> setting.set(dropdown.get());

        reset(table, setting, () -> dropdown.set(setting.get()));
    }

    private void potionW(WTable table, PotionSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();
        WItemWithLabel item = list.add(theme.itemWithLabel(setting.get().potion, setting.get().potion.getName().getString())).widget();

        WButton button = list.add(theme.button("Select")).expandCellX().widget();
        button.action = () -> {
            WidgetScreen screen = new PotionSettingScreen(theme, setting);
            screen.onClosed(() -> item.set(setting.get().potion));

            mc.openScreen(screen);
        };

        reset(list, setting, () -> item.set(setting.get().potion));
    }

    private void colorW(WTable table, ColorSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();

        WQuad quad = list.add(theme.quad(setting.get())).widget();

        WButton edit = list.add(theme.button(GuiRenderer.EDIT)).widget();
        edit.action = () -> mc.openScreen(new ColorSettingScreen(theme, setting));

        reset(table, setting, () -> quad.color = setting.get());
    }

    private void stringW(WTable table, StringSetting setting) {
        WTextBox textBox = table.add(theme.textBox(setting.get())).expandX().widget();
        textBox.action = () -> setting.set(textBox.get());

        reset(table, setting, () -> textBox.set(setting.get()));
    }

    private void providedStringW(WTable table, ProvidedStringSetting setting) {
        WDropdown<String> dropdown = table.add(theme.dropdown(setting.supplier.get(), setting.get())).expandCellX().widget();
        dropdown.action = () -> setting.set(dropdown.get());

        reset(table, setting, () -> dropdown.set(setting.get()));
    }

    private void blockW(WTable table, BlockSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandX().widget();

        WItem item = list.add(theme.item(setting.get().asItem().getDefaultStack())).widget();

        WButton select = list.add(theme.button("Select")).widget();
        select.action = () -> {
            BlockSettingScreen screen = new BlockSettingScreen(theme, setting);
            screen.onClosed(() -> item.set(setting.get().asItem().getDefaultStack()));

            mc.openScreen(screen);
        };

        reset(table, setting, () -> item.set(setting.get().asItem().getDefaultStack()));
    }

    private void keybindW(WTable table, KeybindSetting setting) {
        WKeybind keybind = table.add(theme.keybind(setting.get(), setting.getDefaultValue().getValue())).expandX().widget();
        keybind.action = setting::changed;
        setting.widget = keybind;
    }

    private void genericW(WTable table, GenericSetting<?> setting) {
        WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
        edit.action = () -> mc.openScreen(setting.get().createScreen(theme));

        reset(table, setting, null);
    }

    private void blockListW(WTable table, BlockListSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new BlockListSettingScreen(theme, setting)));
    }

    private void itemListW(WTable table, ItemListSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new ItemListSettingScreen(theme, setting)));
    }

    private void entityTypeListW(WTable table, EntityTypeListSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new EntityTypeListSettingScreen(theme, setting)));
    }

    private void enchListW(WTable table, EnchListSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new EnchListSettingScreen(theme, setting)));
    }

    private void moduleListW(WTable table, ModuleListSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new ModuleListSettingScreen(theme, setting)));
    }

    private void packetBoolW(WTable table, PacketBoolSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new PacketBoolSettingScreen(theme, setting)));
    }

    private void particleEffectListW(WTable table, ParticleTypeListSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new ParticleTypeListSettingScreen(theme, setting)));
    }

    private void soundEventListW(WTable table, SoundEventListSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new SoundEventListSettingScreen(theme, setting)));
    }

    private void statusEffectW(WTable table, StatusEffectSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new StatusEffectSettingScreen(theme, setting)));
    }

    private void storageBlockListW(WTable table, StorageBlockListSetting setting) {
        selectW(table, setting, () -> mc.openScreen(new StorageBlockListSettingScreen(theme, setting)));
    }

    private void blockDataSettingW(WTable table, BlockDataSetting<?> setting) {
        WButton button = table.add(theme.button(GuiRenderer.EDIT)).expandCellX().widget();
        button.action = () -> mc.openScreen(new BlockDataSettingScreen(theme, setting));

        reset(table, setting, null);
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
