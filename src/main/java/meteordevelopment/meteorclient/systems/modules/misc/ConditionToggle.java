/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;

import java.util.ArrayList;
import java.util.List;

//ᓚᘏᗢ
public class ConditionToggle extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> death = sgGeneral.add(new BoolSetting.Builder()
            .name("death-toggle")
            .description("Toggles modules when you die.")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Module>> deathOnToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("toggle-on-on-death")
            .description("Which modules to activate on death.")
            .visible(death::get)
            .build()
    );

    private final Setting<List<Module>> deathOffToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("toggle-off-on-death")
            .description("Which modules to toggle off on death.")
            .visible(death::get)
            .build()
    );

    private final Setting<Boolean> logout = sgGeneral.add(new BoolSetting.Builder()
            .name("logout-toggle")
            .description("Toggles modules when you log out.")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Module>> logoutOnToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("toggle-on-on-logout")
            .description("Which modules to activate on logout.")
            .visible(logout::get)
            .build()
    );

    private final Setting<List<Module>> logoutOffToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("toggle-off-on-logout")
            .description("Which modules to toggle off on logout.")
            .visible(logout::get)
            .build()
    );

    private final Setting<Boolean> damage = sgGeneral.add(new BoolSetting.Builder()
            .name("damage-toggle")
            .description("Toggles modules when you take damage.")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Module>> damageOnToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("toggle-on-on-damage")
            .description("Which modules to activate on damage.")
            .visible(damage::get)
            .build()
    );

    private final Setting<List<Module>> damageOffToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("toggle-off-on-damage")
            .description("Which modules to toggle off on damage.")
            .visible(damage::get)
            .build()
    );

    private final Setting<Boolean> player = sgGeneral.add(new BoolSetting.Builder()
            .name("player-toggle")
            .description("Toggles modules when players enter your render distance.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignores friends entering your render distance.")
            .defaultValue(true)
            .visible(player::get)
            .build()
    );

    private final Setting<List<Module>> playerOnToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("toggle-on-on-player")
            .description("Which modules to activate on player.")
            .visible(player::get)
            .build()
    );

    private final Setting<List<Module>> playerOffToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("toggle-off-on-player")
            .description("Which modules to toggle off on player.")
            .visible(player::get)
            .build()
    );

    public ConditionToggle() {
        super(Categories.Misc, "condition-toggle", "toggles modules based on conditions");
    }

    //death toggle
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event)  {
        if (event.packet instanceof DeathMessageS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.getEntityId());
            if (entity == mc.player && death.get()) {
                toggleModules(deathOnToggleModules.get(), deathOffToggleModules.get());
            }
        }
    }

    //damage toggle
    @EventHandler
    private void onDamage(DamageEvent event) {
        if (event.entity.getUuid() == null) return;
        if (!event.entity.getUuid().equals(mc.player.getUuid())) return;

        if (damage.get()) {
            toggleModules(damageOnToggleModules.get(), damageOffToggleModules.get());
        }
    }

    //logout toggle
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (logout.get()) {
            toggleModules(logoutOffToggleModules.get(), logoutOnToggleModules.get());
        }
    }

    //player toggle
    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) {
                if (entity.getUuid() != mc.player.getUuid()) {
                    if (!ignoreFriends.get() && entity != mc.player) {
                        if (player.get()) {
                            toggleModules(playerOnToggleModules.get(), playerOffToggleModules.get());
                        }
                    } else if (ignoreFriends.get() && !Friends.get().isFriend((PlayerEntity) entity)) {
                        if (player.get()) {
                            toggleModules(playerOnToggleModules.get(), playerOffToggleModules.get());
                        }
                    }
                }
            }
        }
    }

    //toggle modules
    private void toggleModules(List<Module> onModules, List<Module> offModules) {
        for (Module module : offModules) {
            if (module.isActive()) {
                module.toggle();
            }
        }
        for (Module module : onModules) {
            if (!module.isActive()) {
                module.toggle();
            }
        }
    }
}
