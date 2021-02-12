/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.screens.settings;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WIntTextBox;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.misc.Names;
import net.minecraft.entity.effect.StatusEffect;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StatusEffectSettingScreen extends WindowScreen {
    private final Setting<Object2IntMap<StatusEffect>> setting;
    private final WTextBox filter;

    private String filterText = "";

    public StatusEffectSettingScreen(Setting<Object2IntMap<StatusEffect>> setting) {
        super("Select Potions", true);

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 200);
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.getText().trim();

            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        add(filter).fillX().expandX();
        row();

        List<StatusEffect> statusEffects = new ArrayList<>(setting.get().keySet());
        statusEffects.sort(Comparator.comparing(Names::get));

        WTable table = add(new WTable()).expandX().fillX().getWidget();

        for (StatusEffect statusEffect : statusEffects) {
            String name = Names.get(statusEffect);
            if (!StringUtils.containsIgnoreCase(name, filterText)) continue;

            table.add(new WLabel(name));
            WIntTextBox level = table.add(new WIntTextBox(setting.get().getInt(statusEffect), 50)).fillX().right().getWidget();
            level.action = () -> {
                setting.get().put(statusEffect, level.getValue());
                setting.changed();
            };

            table.row();
        }
    }
}
