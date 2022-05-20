/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.CustomStatListSetting;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

import java.util.*;

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
        .description("How frequently to update statistics, in milliseconds. (0 to not update automatically) (sends one packet)")
        .range(0, 40000)
        .defaultValue(5000)
        .noSlider()
        .build()
    );

    private Long lastRequested = 0L;
    private final List<String> cachedStats = new ArrayList<>();

    public StatisticsHud(HUD hud) {
        super(hud, "statistics", "Displays selected in-game statistics.", true);
    }
    public void updateIdentifier(List<Identifier> i) {
        cachedStats.clear();
        genStats(i);
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof StatisticsS2CPacket) {
            Map<Stat<?>, Integer> m = ((StatisticsS2CPacket) event.packet).getStatMap();
        }
    }
    private void genStats(List<Identifier> l) {
        if (mc.player == null) {
            return;
        }
        for (Identifier i : l) {
            if (i == null) {
                cachedStats.add("Null stat?!");
                continue;
            }
            if (!Stats.CUSTOM.hasStat(i)) {
                if(Stats.CUSTOM.getRegistry().containsId(i)) {
                    cachedStats.add(String.format("Stat [%s] not initialized but present in registry. (%s)\n", i, Stats.CUSTOM.getRegistry().getId(i)));
                } else {
                    cachedStats.add(String.format("Stat [%s] not initialized and NOT present in registry.\n", i));
                }
                continue;
            }
            Stat<Identifier> s = Stats.CUSTOM.getOrCreateStat(i);
//            String label = labelsCache.computeIfAbsent(s, identifierStat -> new TranslatableText("stat." + identifierStat.getValue().toString().replace(':', '.')).getString());
            cachedStats.add(Names.get(s) + ": " + s.format(mc.player.getStatHandler().getStat(s)));
            MeteorClient.LOG.info("Updated: {}", i);
        }
    }

    @Override
    public void update(HudRenderer renderer) {
        if (this.active && requestDelay.get() != 0 && lastRequested + requestDelay.get() < System.currentTimeMillis() && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(REQUEST_STATS));
            lastRequested = System.currentTimeMillis();
        }
        List<Identifier> l = sIdentifier.get();
        cachedStats.clear();
        genStats(l);
        box.height = renderer.textHeight()*(l.size()+1);
        box.width = 2;
        for (String c : cachedStats) {
            double w = renderer.textWidth(c);
            if (box.width < w) {
                box.width = w;
            }
        }
    }
    @Override
    public void render(HudRenderer renderer) {
        if(cachedStats.size() == 0) {
            renderer.text("0 cached stats?", box.getX(), box.getY(), hud.primaryColor.get());
            return;
        }
        renderer.text(String.format("Stats: (%d)", cachedStats.size()), box.getX(), box.getY(), hud.primaryColor.get());
        for (int i = 1;  i <= cachedStats.size(); i++) {
            renderer.text(cachedStats.get(i - 1), box.getX(), box.getY() + renderer.textHeight()*i, hud.primaryColor.get());
        }
    }
}
