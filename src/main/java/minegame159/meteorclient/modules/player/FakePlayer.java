/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.entity.FakePlayerUtils;
import net.minecraft.entity.player.PlayerEntity;

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
            .description("Copies your exact inventory to the fake player.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> glowing = sgGeneral.add(new BoolSetting.Builder()
            .name("glowing")
            .description("Grants the fake player a glowing effect.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("The fake player's default health.")
            .defaultValue(20)
            .min(1)
            .sliderMax(100)
            .build()
    );

    public final Setting<Boolean> idInNametag = sgGeneral.add(new BoolSetting.Builder()
            .name("iD-in-nametag")
            .description("Displays the fake player's ID inside its nametag.")
            .defaultValue(true)
            .build()
    );

    public FakePlayer() {
        super(Categories.Player, "fake-player", "Spawns a client-side fake player for testing usages.");
    }

    @Override
    public void onActivate() {
        FakePlayerUtils.ID = 0;
    }

    @Override
    public void onDeactivate() {
        FakePlayerUtils.ID = 0;
        FakePlayerUtils.clearFakePlayers();
    }

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();

        WButton spawn = table.add(new WButton("Spawn")).getWidget();
        spawn.action = FakePlayerUtils::spawnFakePlayer;

        WButton clear = table.add(new WButton("Clear")).getWidget();
        clear.action = () -> {
            if (isActive()) FakePlayerUtils.clearFakePlayers();
        };

        return table;
    }

    public boolean showID(PlayerEntity entity) {
        return isActive() && idInNametag.get() && entity instanceof FakePlayerEntity;
    }

    @Override
    public String getInfoString() {
        if (FakePlayerUtils.getPlayers() != null) return String.valueOf(FakePlayerUtils.getPlayers().size());
        return null;
    }
}