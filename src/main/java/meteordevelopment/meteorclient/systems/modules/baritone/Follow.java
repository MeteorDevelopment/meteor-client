/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.baritone;

import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/** One-shot command: press its keybind to have Baritone follow the configured target. */
public class Follow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Target> target = sgGeneral.add(new EnumSetting.Builder<Target>()
        .name("target")
        .description("Who to follow.")
        .defaultValue(Target.NearestPlayer)
        .build()
    );

    private final Setting<String> playerName = sgGeneral.add(new StringSetting.Builder()
        .name("player-name")
        .description("Name of the player to follow.")
        .defaultValue("")
        .visible(() -> target.get() == Target.PlayerByName)
        .build()
    );

    public Follow() {
        super(Categories.Baritone, "follow", "Has Baritone follow the configured target.");
    }

    @Override
    public void onActivate() {
        if (BaritoneUtil.check(this)) PathManagers.get().follow(predicate());

        toggle();
    }

    private Predicate<Entity> predicate() {
        return switch (target.get()) {
            case NearestPlayer -> entity -> entity instanceof Player && entity != mc.player;
            case NearestEntity -> entity -> entity != mc.player && entity.isAlive();
            case PlayerByName -> {
                String name = playerName.get();
                yield entity -> entity instanceof Player && entity.getName().getString().equalsIgnoreCase(name);
            }
        };
    }

    public enum Target {
        NearestPlayer,
        NearestEntity,
        PlayerByName
    }
}
