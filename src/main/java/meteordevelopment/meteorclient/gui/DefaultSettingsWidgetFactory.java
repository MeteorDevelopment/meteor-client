/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.settings.*;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLabel;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.widgets.*;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.*;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.resource.language.I18n;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DefaultSettingsWidgetFactory extends SettingsWidgetFactory {
    private static final SettingColor WHITE = new SettingColor();

    public DefaultSettingsWidgetFactory(GuiTheme theme) {
        super(theme);

        factories.put(BoolSetting.class, (container, setting) -> boolW(container, (BoolSetting) setting));
        factories.put(IntSetting.class, (container, setting) -> intW(container, (IntSetting) setting));
        factories.put(DoubleSetting.class, (container, setting) -> doubleW(container, (DoubleSetting) setting));
        factories.put(StringSetting.class, (container, setting) -> stringW(container, (StringSetting) setting));
        factories.put(EnumSetting.class, (container, setting) -> enumW(container, (EnumSetting<? extends Enum<?>>) setting));
        factories.put(ProvidedStringSetting.class, (container, setting) -> providedStringW(container, (ProvidedStringSetting) setting));
        factories.put(GenericSetting.class, (container, setting) -> genericW(container, (GenericSetting<?>) setting));
        factories.put(ColorSetting.class, (container, setting) -> colorW(container, (ColorSetting) setting));
        factories.put(KeybindSetting.class, (container, setting) -> keybindW(container, (KeybindSetting) setting));
        factories.put(BlockSetting.class, (container, setting) -> blockW(container, (BlockSetting) setting));
        factories.put(BlockListSetting.class, (container, setting) -> blockListW(container, (BlockListSetting) setting));
        factories.put(ItemSetting.class, (container, setting) -> itemW(container, (ItemSetting) setting));
        factories.put(ItemListSetting.class, (container, setting) -> itemListW(container, (ItemListSetting) setting));
        factories.put(EntityTypeListSetting.class, (container, setting) -> entityTypeListW(container, (EntityTypeListSetting) setting));
        factories.put(EnchantmentListSetting.class, (container, setting) -> enchantmentListW(container, (EnchantmentListSetting) setting));
        factories.put(ModuleListSetting.class, (container, setting) -> moduleListW(container, (ModuleListSetting) setting));
        factories.put(PacketListSetting.class, (container, setting) -> packetListW(container, (PacketListSetting) setting));
        factories.put(ParticleTypeListSetting.class, (container, setting) -> particleTypeListW(container, (ParticleTypeListSetting) setting));
        factories.put(SoundEventListSetting.class, (container, setting) -> soundEventListW(container, (SoundEventListSetting) setting));
        factories.put(StatusEffectAmplifierMapSetting.class, (container, setting) -> statusEffectAmplifierMapW(container, (StatusEffectAmplifierMapSetting) setting));
        factories.put(StatusEffectListSetting.class, (container, setting) -> statusEffectListW(container, (StatusEffectListSetting) setting));
        factories.put(StorageBlockListSetting.class, (container, setting) -> storageBlockListW(container, (StorageBlockListSetting) setting));
        factories.put(ScreenHandlerListSetting.class, (container, setting) -> screenHandlerListW(container, (ScreenHandlerListSetting) setting));
        factories.put(BlockDataSetting.class, (container, setting) -> blockDataW(container, (BlockDataSetting<?>) setting));
        factories.put(PotionSetting.class, (container, setting) -> potionW(container, (PotionSetting) setting));
        factories.put(StringListSetting.class, (container, setting) -> stringListW(container, (StringListSetting) setting));
        factories.put(BlockPosSetting.class, (container, setting) -> blockPosW(container, (BlockPosSetting) setting));
        factories.put(ColorListSetting.class, (container, setting) -> colorListW(container, (ColorListSetting) setting));
        factories.put(FontFaceSetting.class, (container, setting) -> fontW(container, (FontFaceSetting) setting));
        factories.put(Vector3dSetting.class, (container, setting) -> vector3dW(container, (Vector3dSetting) setting));
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
        for (RemoveInfo removeInfo : removeInfoList) {
            removeInfo.remove(list);
        }

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
            if (!Strings.CI.contains(setting.title, filter)) continue;

            boolean visible = setting.isVisible();
            setting.lastWasVisible = visible;
            if (!visible) {
                if (removeInfo == null) removeInfo = new RemoveInfo(section, table);
                removeInfo.markRowForRemoval();
            }

            table.add(theme.label(setting.title)).top().marginTop(settingTitleTopMargin()).widget().tooltip = setting.description;

            Factory factory = getFactory(setting.getClass());
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

    private void boolW(WContainer container, BoolSetting setting) {
        WCheckbox checkbox = container.add(theme.checkbox(setting.get())).expandCellX().widget();
        checkbox.action = () -> setting.set(checkbox.checked);

        reset(container, setting, () -> checkbox.checked = setting.get());
    }

    private void intW(WContainer container, IntSetting setting) {
        WIntEdit edit = container.add(theme.intEdit(setting.get(), setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.noSlider)).expandX().widget();

        edit.action = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        reset(container, setting, () -> edit.set(setting.get()));
    }

    private void doubleW(WContainer container, DoubleSetting setting) {
        WDoubleEdit edit = theme.doubleEdit(setting.get(), setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.decimalPlaces, setting.noSlider);
        container.add(edit).expandX();

        Runnable action = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        if (setting.onSliderRelease) edit.actionOnRelease = action;
        else edit.action = action;

        reset(container, setting, () -> edit.set(setting.get()));
    }

    private void stringW(WContainer container, StringSetting setting) {
        CharFilter filter = setting.filter == null ? (text, c) -> true : setting.filter;
        Cell<WTextBox> cell = container.add(theme.textBox(setting.get(), setting.placeholder, filter, setting.renderer));
        if (setting.wide) cell.minWidth(Utils.getWindowWidth() - Utils.getWindowWidth() / 4.0);

        WTextBox textBox = cell.expandX().widget();
        textBox.action = () -> setting.set(textBox.get());

        reset(container, setting, () -> textBox.set(setting.get()));
    }

    private void stringListW(WContainer container, StringListSetting setting) {
        WTable wcontainer = container.add(theme.table()).expandX().widget();
        StringListSetting.fillTable(theme, wcontainer, setting);
    }

    private <T extends Enum<?>> void enumW(WContainer container, EnumSetting<T> setting) {
        WDropdown<T> dropdown = container.add(theme.dropdown(setting.get())).expandCellX().widget();
        dropdown.action = () -> setting.set(dropdown.get());

        reset(container, setting, () -> dropdown.set(setting.get()));
    }

    private void providedStringW(WContainer container, ProvidedStringSetting setting) {
        WDropdown<String> dropdown = container.add(theme.dropdown(setting.supplier.get(), setting.get())).expandCellX().widget();
        dropdown.action = () -> setting.set(dropdown.get());

        reset(container, setting, () -> dropdown.set(setting.get()));
    }

    private void genericW(WContainer container, GenericSetting<?> setting) {
        WButton edit = container.add(theme.button(GuiRenderer.EDIT)).widget();
        edit.action = () -> mc.setScreen(setting.createScreen(theme));

        reset(container, setting, null);
    }

    private void colorW(WContainer container, ColorSetting setting) {
        WHorizontalList list = container.add(theme.horizontalList()).expandX().widget();

        WQuad quad = list.add(theme.quad(setting.get())).widget();

        WButton edit = list.add(theme.button(GuiRenderer.EDIT)).widget();
        edit.action = () -> mc.setScreen(new ColorSettingScreen(theme, setting));

        reset(container, setting, () -> quad.color = setting.get());
    }

    private void keybindW(WContainer container, KeybindSetting setting) {
        WHorizontalList list = container.add(theme.horizontalList()).expandX().widget();

        WKeybind keybind = list.add(theme.keybind(setting.get(), setting.getDefaultValue())).expandX().widget();
        keybind.action = setting::onChanged;
        setting.widget = keybind;

        WButton reset = list.add(theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
        reset.action = keybind::resetBind;
        reset.tooltip = "Reset";
    }

    private void blockW(WContainer container, BlockSetting setting) {
        WHorizontalList list = container.add(theme.horizontalList()).expandX().widget();

        WItem item = list.add(theme.item(setting.get().asItem().getDefaultStack())).widget();

        WButton select = list.add(theme.button("Select")).widget();
        select.action = () -> {
            BlockSettingScreen screen = new BlockSettingScreen(theme, setting);
            screen.onClosed(() -> item.set(setting.get().asItem().getDefaultStack()));

            mc.setScreen(screen);
        };

        reset(container, setting, () -> item.set(setting.get().asItem().getDefaultStack()));
    }

    private void blockPosW(WContainer container, BlockPosSetting setting) {
        WBlockPosEdit edit = container.add(theme.blockPosEdit(setting.get())).expandX().widget();

        edit.actionOnRelease = () -> {
            if (!setting.set(edit.get())) edit.set(setting.get());
        };

        reset(container, setting, () -> edit.set(setting.get()));
    }

    private void blockListW(WContainer container, BlockListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new BlockListSettingScreen(theme, setting)));
    }

    private void itemW(WContainer container, ItemSetting setting) {
        WHorizontalList list = container.add(theme.horizontalList()).expandX().widget();

        WItem item = list.add(theme.item(setting.get().asItem().getDefaultStack())).widget();

        WButton select = list.add(theme.button("Select")).widget();
        select.action = () -> {
            ItemSettingScreen screen = new ItemSettingScreen(theme, setting);
            screen.onClosed(() -> item.set(setting.get().getDefaultStack()));

            mc.setScreen(screen);
        };

        reset(container, setting, () -> item.set(setting.get().getDefaultStack()));
    }

    private void itemListW(WContainer container, ItemListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new ItemListSettingScreen(theme, setting)));
    }

    private void entityTypeListW(WContainer container, EntityTypeListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new EntityTypeListSettingScreen(theme, setting)));
    }

    private void enchantmentListW(WContainer container, EnchantmentListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new EnchantmentListSettingScreen(theme, setting)));
    }

    private void moduleListW(WContainer container, ModuleListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new ModuleListSettingScreen(theme, setting)));
    }

    private void packetListW(WContainer container, PacketListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new PacketBoolSettingScreen(theme, setting)));
    }

    private void particleTypeListW(WContainer container, ParticleTypeListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new ParticleTypeListSettingScreen(theme, setting)));
    }

    private void soundEventListW(WContainer container, SoundEventListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new SoundEventListSettingScreen(theme, setting)));
    }

    private void statusEffectAmplifierMapW(WContainer container, StatusEffectAmplifierMapSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new StatusEffectAmplifierMapSettingScreen(theme, setting)));
    }

    private void statusEffectListW(WContainer container, StatusEffectListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new StatusEffectListSettingScreen(theme, setting)));
    }

    private void storageBlockListW(WContainer container, StorageBlockListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new StorageBlockListSettingScreen(theme, setting)));
    }

    private void screenHandlerListW(WContainer container, ScreenHandlerListSetting setting) {
        selectW(container, setting, () -> mc.setScreen(new ScreenHandlerSettingScreen(theme, setting)));
    }

    private void blockDataW(WContainer container, BlockDataSetting<?> setting) {
        WButton button = container.add(theme.button(GuiRenderer.EDIT)).expandCellX().widget();
        button.action = () -> mc.setScreen(new BlockDataSettingScreen<>(theme, setting));

        reset(container, setting, null);
    }

    private void potionW(WContainer container, PotionSetting setting) {
        WHorizontalList list = container.add(theme.horizontalList()).expandX().widget();
        WItemWithLabel item = list.add(theme.itemWithLabel(setting.get().potion, I18n.translate(setting.get().potion.getItem().getTranslationKey()))).widget();

        WButton button = list.add(theme.button("Select")).expandCellX().widget();
        button.action = () -> {
            WidgetScreen screen = new PotionSettingScreen(theme, setting);
            screen.onClosed(() -> item.set(setting.get().potion));

            mc.setScreen(screen);
        };

        reset(list, setting, () -> item.set(setting.get().potion));
    }

    private void fontW(WContainer container, FontFaceSetting setting) {
        WHorizontalList list = container.add(theme.horizontalList()).expandX().widget();
        WLabel label = list.add(theme.label(setting.get().info.family())).widget();

        WButton button = list.add(theme.button("Select")).expandCellX().widget();
        button.action = () -> {
            WidgetScreen screen = new FontFaceSettingScreen(theme, setting);
            screen.onClosed(() -> label.set(setting.get().info.family()));

            mc.setScreen(screen);
        };

        reset(list, setting, () -> label.set(Fonts.DEFAULT_FONT.info.family()));
    }

    private void colorListW(WContainer container, ColorListSetting setting) {
        WTable tab = container.add(theme.table()).expandX().widget();
        WTable t = tab.add(theme.table()).expandX().widget();
        tab.row();

        colorListWFill(t, setting);

        WPlus add = tab.add(theme.plus()).expandCellX().widget();
        add.action = () -> {
            setting.get().add(new SettingColor());
            setting.onChanged();

            t.clear();
            colorListWFill(t, setting);
        };

        reset(tab, setting, () -> {
            t.clear();
            colorListWFill(t, setting);
        });
    }

    private void colorListWFill(WTable t, ColorListSetting setting) {
        int i = 0;
        for (SettingColor color : setting.get()) {
            int _i = i;

            t.add(theme.label(i + ":"));

            t.add(theme.quad(color)).widget();

            WButton edit = t.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.action = () -> {
                SettingColor defaultValue = WHITE;
                if (_i < setting.getDefaultValue().size()) defaultValue = setting.getDefaultValue().get(_i);

                ColorSetting set = new ColorSetting(setting.name, setting.description, defaultValue, settingColor -> {
                    setting.get().get(_i).set(settingColor);
                    setting.onChanged();
                }, null, null);
                set.set(setting.get().get(_i));
                mc.setScreen(new ColorSettingScreen(theme, set));
            };

            WMinus remove = t.add(theme.minus()).expandCellX().right().widget();
            remove.action = () -> {
                setting.get().remove(_i);
                setting.onChanged();

                t.clear();
                colorListWFill(t, setting);
            };

            t.row();
            i++;
        }
    }

    private void vector3dW(WContainer container, Vector3dSetting setting) {
        WTable internal = container.add(theme.table()).expandX().widget();

        WDoubleEdit x = addVectorComponent(internal, "X", setting.get().x, val -> setting.get().x = val, setting);
        WDoubleEdit y = addVectorComponent(internal, "Y", setting.get().y, val -> setting.get().y = val, setting);
        WDoubleEdit z = addVectorComponent(internal, "Z", setting.get().z, val -> setting.get().z = val, setting);

        reset(container, setting, () -> {
            x.set(setting.get().x);
            y.set(setting.get().y);
            z.set(setting.get().z);
        });
    }

    private WDoubleEdit addVectorComponent(WTable table, String label, double value, Consumer<Double> update, Vector3dSetting setting) {
        table.add(theme.label(label + ": "));

        WDoubleEdit component = table.add(theme.doubleEdit(value, setting.min, setting.max, setting.sliderMin, setting.sliderMax, setting.decimalPlaces, setting.noSlider)).expandX().widget();
        if (setting.onSliderRelease) {
            component.actionOnRelease = () -> {
                update.accept(component.get());
                setting.onChanged();
            };
        } else {
            component.action = () -> {
                update.accept(component.get());
                setting.onChanged();
            };
        }

        table.row();

        return component;
    }

    // Other

    private void selectW(WContainer c, Setting<?> setting, Runnable action) {
        boolean addCount = WSelectedCountLabel.getSize(setting) != -1;

        WContainer c2 = c;
        if (addCount) {
            c2 = c.add(theme.horizontalList()).expandCellX().widget();
            ((WHorizontalList) c2).spacing *= 2;
        }

        WButton button = c2.add(theme.button("Select")).expandCellX().widget();
        button.action = action;

        if (addCount) c2.add(new WSelectedCountLabel(setting).color(theme.textSecondaryColor()));

        reset(c, setting, null);
    }

    private void reset(WContainer c, Setting<?> setting, Runnable action) {
        WButton reset = c.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();
            if (action != null) action.run();
        };
        reset.tooltip = "Reset";
    }

    private static class WSelectedCountLabel extends WMeteorLabel {
        private final Setting<?> setting;
        private int lastSize = -1;

        public WSelectedCountLabel(Setting<?> setting) {
            super("", false);

            this.setting = setting;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            int size = getSize(setting);

            if (size != lastSize) {
                set("(" + size + " selected)");
                lastSize = size;
            }

            super.onRender(renderer, mouseX, mouseY, delta);
        }

        public static int getSize(Setting<?> setting) {
            if (setting.get() instanceof Collection<?> collection) return collection.size();
            if (setting.get() instanceof Map<?, ?> map) return map.size();

            return -1;
        }
    }
}
