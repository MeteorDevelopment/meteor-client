/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.BoatMoveEvent;
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

public class BoatGlitch extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> toggleAfter = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-after")
            .description("Disables the module when finished.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> remount = sgGeneral.add(new BoolSetting.Builder()
            .name("remount")
            .description("Remounts the boat when finished.")
            .defaultValue(true)
            .build()
    );

    private Entity boat = null;
    private int dismountTicks = 0;
    private int remountTicks = 0;
    private boolean dontPhase = true;
    private boolean boatPhaseEnabled;

    public BoatGlitch() {
        super(Categories.Movement, "Boat Glitch", "Glitches your boat into the block beneath you.  Dismount to trigger.");
    }

    @Override
    public void onActivate() {
        dontPhase = true;
        dismountTicks = 0;
        remountTicks = 0;
        boat = null;
        if (Modules.get().isActive(BoatPhase.class)) {
            boatPhaseEnabled = true;
            Modules.get().get(BoatPhase.class).toggle();
        }
        else {
            boatPhaseEnabled = false;
        }
    }

    @Override
    public void onDeactivate() {
        if (boat != null) {
            boat.noClip = false;
            boat = null;
        }
        if (boatPhaseEnabled && !(Modules.get().isActive(BoatPhase.class))) {
            Modules.get().get(BoatPhase.class).toggle();
        }
    }

    @EventHandler
    private void onBoatMove(BoatMoveEvent event) {
        if (dismountTicks == 0 && !dontPhase) {
            if (boat != event.boat) {
                if (boat != null) {
                    boat.noClip = false;
                }
                if (mc.player.getVehicle() != null && event.boat == mc.player.getVehicle()) {
                    boat = event.boat;
                }
                else {
                    boat = null;
                }
            }
            if (boat != null) {
                boat.noClip = true;
                boat.pushSpeedReduction = 1;
                dismountTicks = 5;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (dismountTicks > 0) {
            dismountTicks--;
            if (dismountTicks == 0) {
                if (boat != null) {
                    boat.noClip = false;
                    if (toggleAfter.get() && !remount.get()) {
                        toggle();
                    }
                    else if (remount.get()) {
                        remountTicks = 5;
                    }
                }
                dontPhase = true;
            }
        }
        if (remountTicks > 0) {
            remountTicks--;
            if (remountTicks == 0) {
                mc.getNetworkHandler().sendPacket(new PlayerInteractEntityC2SPacket(boat, Hand.MAIN_HAND, false));
                if (toggleAfter.get()) {
                    toggle();
                }
            }
        }
    }
    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.key == mc.options.keySneak.getDefaultKey().getCode() && event.action == KeyAction.Press) {
            if (mc.player.getVehicle() != null && mc.player.getVehicle().getType().equals(EntityType.BOAT)) {
                dontPhase = false;
                boat = null;
            }
        }
    }
}