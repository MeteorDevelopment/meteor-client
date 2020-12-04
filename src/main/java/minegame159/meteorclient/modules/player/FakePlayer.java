/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.FakePlayerEntity;
import net.minecraft.util.Pair;

import java.util.ArrayList;

public class FakePlayer extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("The name of the fake player.")
            .defaultValue("MeteorOnCrack")
            .build()
    );

    private final Setting<Boolean> copyInv = sgGeneral.add(new BoolSetting.Builder()
            .name("copy-inv")
            .description("Copies your inventory to the fake player.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> glowing = sgGeneral.add(new BoolSetting.Builder()
            .name("glowing")
            .description("Forces the fake player have the glowing effect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("Set the health of the fake player.")
            .defaultValue(20)
            .min(1)
            .sliderMax(100)
            .build()
    );

    private final Setting<Boolean> idInNametag = sgGeneral.add(new BoolSetting.Builder()
            .name("id-in-nametag")
            .description("Renders the fake player's ID in it's nametag.")
            .defaultValue(true)
            .build()
    );

    public FakePlayer() {
        super(Category.Player, "fake-player", "Spawns a clientside fake player.");
    }

    public static ArrayList<Pair<FakePlayerEntity, Integer>> players = new ArrayList<Pair<FakePlayerEntity, Integer>>();
    private int ID;

    public ArrayList<Pair<FakePlayerEntity, Integer>> getPlayers() {
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
            Chat.info(this, "Spawned a fake player with the ID of (highlight)" + ID);
            players.add(new Pair<>(fakePlayer, ID));
            int idlog = new Pair<>(fakePlayer, ID).getRight();
            System.out.println(idlog);
            ID++;
        }
    }

    public void removeFakePlayer(int id) {
        if (isActive()) {
            if (players.isEmpty()) {
                Chat.info(this, "No active fake players to remove!");
                return;
            }
            for (Pair<FakePlayerEntity, Integer> player : players) {
                if (player.getRight() == id) {
                    player.getLeft().despawn();
                    Chat.info(this, "Removed a fake player with the ID of (highlight)" + id);
                }
            }
        }
    }

    public void clearFakePlayers( boolean shouldCheckActive) {
        if (shouldCheckActive && isActive()) {
            if (players.isEmpty()) {
                Chat.info(this, "No active fake players to remove!");
                return;
            } else {
                for (Pair<FakePlayerEntity, Integer> player : players) {
                    player.getLeft().despawn();
                    Chat.info(this, "Removed a fake player with the ID of (highlight)" + player.getRight());
                }

            }
        } else if (!shouldCheckActive) {
            for (Pair<FakePlayerEntity, Integer> player : players) {
                player.getLeft().despawn();
                Chat.info(this, "Removed a fake player with the ID of (highlight)" + player.getRight());
            }
        }
        players.clear();
    }

    public ArrayList<Pair<FakePlayerEntity, Integer>> getFakePlayerEntities() {
        if (!players.isEmpty()) {
            return players;
        } else return null;
    }

    public String getName() {
        return name.get();
    }

    public int getID(FakePlayerEntity entity) {
        int id = -1;

        if (!players.isEmpty()) {
            for (Pair<FakePlayerEntity, Integer> player : players) {
                if (player.getLeft() == entity) id = player.getRight();
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
