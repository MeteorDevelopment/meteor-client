/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

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
        if (mc.player.getInventory().armor.get(3).isOf(Blocks.CARVED_PUMPKIN.asItem()) || mc.player.getAbilities().creativeMode) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndermanEntity enderman) || !enderman.isAlive() || !mc.player.canSee(enderman)) continue;

            switch (lookMode.get()) {
                case Away -> {
                    if (enderman.isAngry() && stun.get()) Rotations.rotate(Rotations.getYaw(enderman), Rotations.getPitch(enderman, Target.Head), -75, null);
                    else if (angleCheck(enderman)) Rotations.rotate(mc.player.getYaw(), 90, -75, null);
                }
                case At -> {
                    if (!enderman.isAngry()) Rotations.rotate(Rotations.getYaw(enderman), Rotations.getPitch(enderman, Target.Head), -75, null);
                }
            }
        }
    }

    /**
     * @see EndermanEntity#isPlayerStaring(PlayerEntity)
     */
    private boolean angleCheck(EndermanEntity entity) {
        Vec3d vec3d = mc.player.getRotationVec(1.0F).normalize();
        Vec3d vec3d2 = new Vec3d(entity.getX() - mc.player.getX(), entity.getEyeY() - mc.player.getEyeY(), entity.getZ() - mc.player.getZ());

        double d = vec3d2.length();
        vec3d2 = vec3d2.normalize();
        double e = vec3d.dotProduct(vec3d2);

        return e > 1.0D - 0.025D / d;
    }

    public enum Mode {
        At,
        Away
    }
}
