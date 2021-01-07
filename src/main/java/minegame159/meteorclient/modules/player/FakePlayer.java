/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.player.Chat;

import java.util.HashMap;
import java.util.Map;

public class FakePlayer extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("The name of the fake player.")
            .defaultValue("MeteorOnCrack")
            .build()
    );

    private final Setting<Boolean> copyInv = sgGeneral.add(new BoolSetting.Builder()
            .name("copy-inv")
            .description("Copies your exact inventory to the fake player.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> glowing = sgGeneral.add(new BoolSetting.Builder()
            .name("glowing")
            .description("Grants the fake player a glowing effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("The fake player's default health.")
            .defaultValue(20)
            .min(1)
            .sliderMax(100)
            .build()
    );

    private final Setting<Boolean> idInNametag = sgGeneral.add(new BoolSetting.Builder()
            .name("id-in-nametag")
            .description("Displays the fake player's ID inside its nametag.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Informs you when a fake player has been spawned or removed.")
            .defaultValue(false)
            .build()
    );

    public FakePlayer() {
        super(Category.Player, "Fake-Player", "Spawns a client-side fake player for testing usages.");
    }

    public static Map<FakePlayerEntity, Integer> players = new HashMap<>();
    private int ID;

    public Map<FakePlayerEntity, Integer> getPlayers() {
        if (!players.isEmpty()) {
            return players;
        } else return null;
    }

    @Override
    public void onActivate() {
        ID = 0;
    }

    @Override
    public void onDeactivate() {
        ID = 0;
        clearFakePlayers(false);
    }

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();

        WButton spawn = table.add(new WButton("Spawn")).getWidget();
        spawn.action = () -> spawnFakePlayer(name.get(), copyInv.get(), glowing.get(), health.get().floatValue());

        WButton clear = table.add(new WButton("Clear")).getWidget();
        clear.action = () -> clearFakePlayers(true);

        return table;
    }

    public void spawnFakePlayer(String name, boolean copyInv, boolean glowing, float health) {
        if (isActive()) {
            if (mc.world == null) return;
            FakePlayerEntity fakePlayer = new FakePlayerEntity(name, copyInv, glowing, health);
            if (chatInfo.get()) Chat.info(this, "Spawned a fakeplayer");
            players.put(fakePlayer, ID);
            ID++;
        }
    }

    public void removeFakePlayer(int id) {
        if (isActive()) {
            if (players.isEmpty()) {
                if (chatInfo.get()) Chat.info(this, "There are no active fake players to remove!");
                return;
            }
            for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                if (player.getValue() == id) {
                    player.getKey().despawn();
                    if (chatInfo.get()) Chat.info(this, "Removed fake player with ID (highlight)" + id);
                }
            }
        }
    }

    public void clearFakePlayers( boolean shouldCheckActive) {
        if (shouldCheckActive && isActive()) {
            if (players.isEmpty()) {
                if (chatInfo.get()) Chat.info(this, "There are no active fake players to remove!");
                return;
            } else {
                for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                    player.getKey().despawn();
                }
                if (chatInfo.get()) Chat.info(this, "Removed all fake players.");

            }
        } else if (!shouldCheckActive) {
            for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                player.getKey().despawn();
            }
            if (chatInfo.get()) Chat.info(this, "Removed all fake players.");
        }
        players.clear();
    }

    public String getName() {
        return name.get();
    }

    public int getID(FakePlayerEntity entity) {
        int id = -1;

        if (!players.isEmpty()) {
            for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                if (player.getKey() == entity) id = player.getValue();
            }
        }

        return id;
    }

    public boolean showID() {
        return idInNametag.get();
    }

    public boolean copyInv() {
        return copyInv.get();
    }

    public boolean setGlowing() {
        return glowing.get();
    }

    public float getHealth() {
        return health.get().floatValue();
    }
}