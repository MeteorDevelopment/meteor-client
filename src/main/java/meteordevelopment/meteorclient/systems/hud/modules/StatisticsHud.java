/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.CustomStatSetting;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import static net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode.REQUEST_STATS;

public class StatisticsHud extends DoubleTextHudElement  {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Stat<Identifier>> sIdentifier = sgGeneral.add(new CustomStatSetting.Builder()
        .name("stat")
        .description("Statistic that you want to display")
        .defaultValue(Stats.CUSTOM.getOrCreateStat(Stats.OPEN_ENDERCHEST))
        .onChanged(this::updateIdentifier)
        .build()
    );
    private final Setting<Integer> requestDelay = sgGeneral.add(new IntSetting.Builder()
        .name("update-delay")
        .description("How frequently to update statistics, in milliseconds. (0 to not update automatically)")
        .range(0, 40000)
        .defaultValue(5000)
        .noSlider()
        .build()
    );

    private Long lastRequested = System.currentTimeMillis();

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
    public void update(HudRenderer renderer) {
        if(this.active && requestDelay.get() != 0 && lastRequested + requestDelay.get() < System.currentTimeMillis() && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(REQUEST_STATS));
            lastRequested = System.currentTimeMillis();
        }
        super.update(renderer);
    }

    @Override
    protected String getRight() {
        if(mc.player != null) {
            return sIdentifier.get().format(mc.player.getStatHandler().getStat(sIdentifier.get()));
        }
        if(isInEditor()) {
            return "69420";
        }
        return "No data";
    }
}
