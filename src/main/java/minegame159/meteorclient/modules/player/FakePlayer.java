/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.FakePlayerEntity;
import net.minecraft.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class FakePlayer extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("Fakeplayer's name.")
            .defaultValue("MeteorOnCrack")
            .build()
    );

    private final Setting<Boolean> copyInv = sgGeneral.add(new BoolSetting.Builder()
            .name("copy-inv")
            .description("Copies your inventory to the Fakeplayer.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> glowing = sgGeneral.add(new BoolSetting.Builder()
            .name("glowing")
            .description("Makes the FakePlayer have the glowing effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("Fakeplayer's health.")
            .defaultValue(20)
            .min(1)
            .sliderMax(100)
            .build()
    );

    private final Setting<Boolean> idInNametag = sgGeneral.add(new BoolSetting.Builder()
            .name("id-in-nametag")
            .description("Renders the Fakeplayer's ID in its nametag.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-info")
            .description("Tells you when a player is added or removed.")
            .defaultValue(false)
            .build()
    );

    public FakePlayer() {
        super(Category.Player, "fake-player", "Spawns a clientside fake player.");
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
            FakePlayerEntity fakePlayer = new FakePlayerEntity(name, copyInv, glowing, health);
            if (chatInfo.get()) Chat.info(this, "Spawned a fakeplayer");
            players.put(fakePlayer, ID);
            int idlog = new Pair<>(fakePlayer, ID).getRight();
            System.out.println(idlog);
            ID++;
        }
    }

    public void removeFakePlayer(int id) {
        if (isActive()) {
            if (players.isEmpty()) {
                if (chatInfo.get()) Chat.info(this, "No active fakeplayers to remove!");
                return;
            }
            for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                if (player.getValue() == id) {
                    player.getKey().despawn();
                    if (chatInfo.get()) Chat.info(this, "Removed a fakeplayer with the id of (highlight)" + id);
                }
            }
        }
    }

    public void clearFakePlayers( boolean shouldCheckActive) {
        if (shouldCheckActive && isActive()) {
            if (players.isEmpty()) {
                if (chatInfo.get()) Chat.info(this, "No active fakeplayers to remove!");
                return;
            } else {
                for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                    player.getKey().despawn();
                }
                if (chatInfo.get()) Chat.info(this, "Removed all fakeplayers.");

            }
        } else if (!shouldCheckActive) {
            for (Map.Entry<FakePlayerEntity, Integer> player : players.entrySet()) {
                player.getKey().despawn();
            }
            if (chatInfo.get()) Chat.info(this, "Removed all fakeplayers.");
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