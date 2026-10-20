/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mc.Vanilla;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class EndermanLook extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> lookMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("look-mode")
        .description("How this module behaves.")
        .defaultValue(Mode.Away)
        .build()
    );

    private final Setting<Boolean> stun = sgGeneral.add(new BoolSetting.Builder()
        .name("stun-hostiles")
        .description("Automatically stares at hostile endermen to stun them in place.")
        .defaultValue(true)
        .visible(() -> lookMode.get() == Mode.Away)
        .build()
    );

    public EndermanLook() {
        super(Categories.World, "enderman-look", "Either looks at all Endermen or prevents you from looking at Endermen.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // if either are true nothing happens when you look at an enderman
        if (mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.GAZE_DISGUISE_EQUIPMENT) || mc.player.getAbilities().instabuild)
            return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!Vanilla.isEnderman(entity) || !entity.isAlive() || !mc.player.hasLineOfSight(entity))
                continue;

            switch (lookMode.get()) {
                case Away -> {
                    if (Vanilla.isCreepy(entity) && stun.get())
                        Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity, Target.Head), -75, null);
                    else if (angleCheck(entity)) Rotations.rotate(mc.player.getYRot(), 90, -75, null);
                }
                case At -> {
                    if (!Vanilla.isCreepy(entity))
                        Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity, Target.Head), -75, null);
                }
            }
        }
    }

    /**
     * @see net.minecraft.world.entity.Entity#isAlive()
     */
    private boolean angleCheck(net.minecraft.world.entity.Entity entity) {
        Vec3 vec3d = mc.player.getViewVector(1.0F).normalize();
        Vec3 vec3d2 = new Vec3(entity.getX() - mc.player.getX(), entity.getEyeY() - mc.player.getEyeY(), entity.getZ() - mc.player.getZ());

        double d = vec3d2.length();
        vec3d2 = vec3d2.normalize();
        double e = vec3d.dot(vec3d2);

        return e > 1.0D - 0.025D / d;
    }

    public enum Mode {
        At,
        Away
    }
}
