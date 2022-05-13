/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.CustomStatSetting;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import net.minecraft.client.gui.screen.StatsListener;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class StatisticsHud extends DoubleTextHudElement  {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Stat<Identifier>> sIdentifier = sgGeneral.add(new CustomStatSetting.Builder()
        .name("stat")
        .description("value")
        .defaultValue(Stats.CUSTOM.getOrCreateStat(Stats.OPEN_ENDERCHEST))
        .onChanged(this::updateIdentifier)
        .build()
    );

    public StatisticsHud(HUD hud) {
        super(hud, "statistics", "Displays selected in-game statistics.", "");
        setLeft(genLeft());
    }

    public String genLeft() {
        return new TranslatableText("stat." + sIdentifier.get().getValue().toString().replace(':', '.')).getString() + ": ";
    }
    public void updateIdentifier(Stat<Identifier> i) {
        setLeft(genLeft());
    }

    @Override
    protected String getRight() {
        if(mc.player != null) {
            return sIdentifier.get().format(mc.player.getStatHandler().getStat(sIdentifier.get()));
        }
        return "No data";
    }
}
