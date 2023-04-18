/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Panic extends Module {

    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgMob = settings.createGroup("Mobs");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    public Panic() {
        super(Categories.Misc, "panic", "Disables modules when a condition is met.");
    }

    private final Setting<Boolean> player_sensitive = sgPlayers.add(new BoolSetting.Builder()
        .name("Player sensitive")
        .description("Disable modules when player is near.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignore_friends = sgPlayers.add(new BoolSetting.Builder()
        .name("Ignore Friends")
        .description("Don't panic when a friend is near.")
        .defaultValue(false)
        .visible(player_sensitive::get)
        .build()
    );

    private final Setting<Integer> range = sgPlayers.add(new IntSetting.Builder()
        .name("Player Range")
        .description("How close a player has to be for modules to be disabled.")
        .defaultValue(4)
        .range(1, 100)
        .sliderMax(100)
        .visible(player_sensitive::get)
        .build()
    );

    private final Setting<Boolean> mob_sensitive = sgMob.add(new BoolSetting.Builder()
        .name("Mob sensitive")
        .description("Disable modules when mobs are near.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> m_range = sgMob.add(new IntSetting.Builder()
        .name("Mob Range")
        .description("How close a mob has to be for modules to be disabled")
        .defaultValue(4)
        .range(1, 100)
        .sliderMax(100)
        .visible(mob_sensitive::get)
        .build()
    );

    private final Setting<Boolean> disable_disconnect = sgMisc.add(new BoolSetting.Builder()
        .name("Disable modules on disconnect.")
        .description("Disable modules when you leave a game.")
        .defaultValue(false)
        .build()
    );

    @EventHandler
    private void onDisconnect(GameLeftEvent event) {
        if (disable_disconnect.get()) {
            panic();
        }
    }


    // Check for potential enemies/mobs
    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (Entity entity : mc.world.getEntities()) {

            if (entity instanceof MobEntity a && mob_sensitive.get()) {
                if (PlayerUtils.isWithin(a, m_range.get())) {
                    panic();
                }
            }

            if (entity instanceof PlayerEntity a && player_sensitive.get()) {
                if (PlayerUtils.isWithin(a, range.get()) && a != mc.player) {
                    if (ignore_friends.get()) {
                        if (!Friends.get().isFriend(a)) {
                            panic();
                        }
                    } else {
                        panic();
                    }
                }
            }
        }
    }

    private void panic() {
        Modules.get().disableAll();
    }

}
