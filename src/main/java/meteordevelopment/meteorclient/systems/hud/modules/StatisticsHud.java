/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.CustomStatListSetting;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;

import static net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode.REQUEST_STATS;

public class StatisticsHud extends HudElement  {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Identifier>> sIdentifier = sgGeneral.add(new CustomStatListSetting.Builder()
        .name("stats")
        .description("Statistics that you want to display")
        .defaultValue(Arrays.asList(Stats.OPEN_ENDERCHEST, Stats.JUMP))
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
    private String cachedStats = "";

    public StatisticsHud(HUD hud) {
        super(hud, "statistics", "Displays selected in-game statistics.", true);
    }
    public void updateIdentifier(List<Identifier> i) {
        cachedStats = genStats(i);
    }
    private String genStats(List<Identifier> l) {
        if(mc.player == null) {
            return "";
        }
        StringBuilder r = new StringBuilder();
        for (Identifier i : l) {
            if(i == null) {
                r.append("Initializing: null");
                continue;
            }
            Stat<Identifier> s = Stats.CUSTOM.getOrCreateStat(i);
            String a = new TranslatableText("stat." + s.getValue().toString().replace(':', '.')).getString() + ": " + s.format(mc.player.getStatHandler().getStat(s));
            if(!r.toString().equals("")) {
                r.append(a);
            } else {
                r.append("\n").append(a);
            }
        }
        return r.toString();
    }

    @Override
    public void update(HudRenderer renderer) {
        if(this.active && requestDelay.get() != 0 && lastRequested + requestDelay.get() < System.currentTimeMillis() && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(REQUEST_STATS));
            lastRequested = System.currentTimeMillis();
        }
        List<Identifier> l = sIdentifier.get();
        if(l.size() == 0) {
            cachedStats = "No statistics selected";
            box.height = renderer.textHeight();
        } else if(isInEditor()) {
            cachedStats = "Times coped: 69420";
            box.height = renderer.textHeight();
        } else {
            cachedStats = genStats(l);
            box.height = renderer.textHeight()*l.size();
        }
        box.width = renderer.textWidth(cachedStats);
    }
    @Override
    public void render(HudRenderer renderer) {
        String[] values = cachedStats.split("\n");
        for (int i = 0;  i < values.length; i++) {
            renderer.text(values[i], box.getX(), box.getY() + renderer.textHeight()*i, hud.primaryColor.get());
        }
    }
}
