/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.player.PlayerPosition;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;

public class FakePlayer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name of the fake player.")
        .defaultValue("seasnail8169")
        .build()
    );

    public final Setting<Boolean> copyInv = sgGeneral.add(new BoolSetting.Builder()
        .name("copy-inv")
        .description("Copies your inventory to the fake player.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("The fake player's default health.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    public final Setting<Boolean> allowDamage = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-damage")
        .description("Allows the fake player spawned to take damage and pop totems")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> loop = sgGeneral.add(new BoolSetting.Builder()
        .name("loop-recording")
        .description("Automatically replays the current recording everytime it ends")
        .defaultValue(true)
        .build()
    );

    public boolean recording = false;
    public boolean playing = false;

    public ArrayList<PlayerPosition> playerPositionsList = new ArrayList<>();


    public FakePlayer() {
        super(Categories.Player, "fake-player", "Spawns a client-side fake player for testing usages. No need to be active.");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (recording) {
            playerPositionsList.add(new PlayerPosition(mc.player.getPos(), mc.player.getYaw(), mc.player.getPitch()));
        } if (playing) {
            if (!playerPositionsList.isEmpty()) {
                if (loop.get()) playerPositionsList.add(playerPositionsList.get(0));
                PlayerPosition playerPosition = playerPositionsList.remove(0);
                FakePlayerManager.forEach(entity -> {entity.updateTrackedPositionAndAngles(playerPosition.pos().x, playerPosition.pos().y, playerPosition.pos().z, playerPosition.yaw(), playerPosition.pitch(),1);
                    entity.updateTrackedHeadRotation(playerPosition.yaw(), 2);
                });
            } else playing = false;
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        fillTable(theme, table);

        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        for (FakePlayerEntity fakePlayer : FakePlayerManager.getFakePlayers()) {
            table.add(theme.label(fakePlayer.getName().getString()));
            WMinus delete = table.add(theme.minus()).expandCellX().right().widget();
            delete.action = () -> {
                FakePlayerManager.remove(fakePlayer);
                table.clear();
                fillTable(theme, table);
            };
            table.row();
        }

        WButton spawn = table.add(theme.button("Spawn")).expandCellX().right().widget();
        spawn.action = () -> {
            if (!this.isActive()) return;
            FakePlayerManager.add(name.get(), health.get(), copyInv.get(), allowDamage.get());
            table.clear();
            fillTable(theme, table);
        };

        WButton record = table.add(theme.button("Record")).right().widget();
        record.action = () -> {
            if (!recording) {
                playerPositionsList.clear();
                this.info("Recording started.");
            } else this.info("Recording stopped.");
            recording = !recording;
        };
        WButton play = table.add(theme.button("Play")).right().widget();
        play.action = () -> {
            if (!playing) {
                if (playerPositionsList.isEmpty()) {
                    this.info("Cannot play an empty recording.");
                    return;
                } else this.info("Recording now playing.");
            } else this.info("Recording stopped playing.");
            playing = !playing;
            if (recording && playing) {
                playing = false;
                this.info("Cannot play while recording.");
            }
        };

        WButton clear = table.add(theme.button("Clear All")).right().widget();
        clear.action = () -> {
            FakePlayerManager.clear();
            table.clear();
            fillTable(theme, table);
        };
    }

    @Override
    public String getInfoString() {
        return String.valueOf(FakePlayerManager.count());
    }
}
