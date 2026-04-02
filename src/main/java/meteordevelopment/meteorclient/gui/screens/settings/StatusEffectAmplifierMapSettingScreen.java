/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class StatusEffectAmplifierMapSettingScreen extends WindowScreen {
    private final Setting<Reference2IntMap<MobEffect>> setting;

    private WTable table;

    private String filterText = "";

    public StatusEffectAmplifierMapSettingScreen(GuiTheme theme, Setting<Reference2IntMap<MobEffect>> setting) {
        super(theme, "Modify Amplifiers");

        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        WTextBox filter = add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            table.clear();
            initTable();
        };

        table = add(theme.table()).expandX().widget();

        initTable();
    }

    private void initTable() {
        List<MobEffect> statusEffects = new ArrayList<>(setting.get().keySet());
        statusEffects.sort(Comparator.comparing(Names::get));

        for (MobEffect statusEffect : statusEffects) {
            String name = Names.get(statusEffect);
            if (!Strings.CI.contains(name, filterText)) continue;

            table.add(theme.itemWithLabel(getPotionStack(statusEffect), name)).expandCellX();

            WIntEdit level = theme.intEdit(setting.get().getInt(statusEffect), 0, Integer.MAX_VALUE, true);
            level.action = () -> {
                setting.get().put(statusEffect, level.get());
                setting.onChanged();
            };

            table.add(level).minWidth(50);
            table.row();
        }
    }

    private ItemStack getPotionStack(MobEffect effect) {
        ItemStack potion = Items.POTION.getDefaultInstance();

        potion.set(
            DataComponents.POTION_CONTENTS,
            new PotionContents(
                potion.get(DataComponents.POTION_CONTENTS).potion(),
                Optional.of(effect.getColor()),
                potion.get(DataComponents.POTION_CONTENTS).customEffects(),
                Optional.empty()
            )
        );

        return potion;
    }
}
