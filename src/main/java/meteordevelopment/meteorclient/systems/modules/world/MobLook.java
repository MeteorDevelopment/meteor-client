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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.math.Vec3d;

public class MobLook extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> endermenMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("endermen-mode")
        .description("Whether to look at/away from endermen")
        .defaultValue(Mode.Away)
        .build()
    );

    private final Setting<Boolean> stunHostiles = sgGeneral.add(new BoolSetting.Builder()
        .name("stun-hostiles")
        .description("Automatically stares at hostile endermen to stun them in place.")
        .defaultValue(true)
        .visible(() -> endermenMode.get() == Mode.Away)
        .build()
    );

    private final Setting<Boolean> stunCreaking = sgGeneral.add(new BoolSetting.Builder()
        .name("stun-creaking")
        .description("Automatically stares at hostile endermen to stun them in place.")
        .defaultValue(true)
        .build()
    );

    public MobLook() {
        super(Categories.World, "mob-look", "Look at/away from endermen and creaking to prevent them from moving.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player.getAbilities().creativeMode) return;

        for (Entity entity : mc.world.getEntities()) {
            if (stunCreaking.get() && entity instanceof CreakingEntity creaking && creaking.isAlive() && mc.player.canSee(creaking)) {
                Rotations.rotate(Rotations.getYaw(creaking), Rotations.getPitch(creaking), -75);
            }

            if (entity instanceof EndermanEntity enderman && enderman.isAlive() && mc.player.canSee(enderman)) {
                if (mc.player.getEquippedStack(EquipmentSlot.HEAD).isOf(Blocks.CARVED_PUMPKIN.asItem())) continue;

                switch (endermenMode.get()) {
                    case Away -> {
                        if (enderman.isAngry() && stunHostiles.get()) {
                            Rotations.rotate(Rotations.getYaw(enderman), Rotations.getPitch(enderman, Target.Head), -75);
                        } else if (playerIsLookingAt(enderman)) {
                            Rotations.rotate(mc.player.getYaw(), 90, -75);
                        }
                    }
                    case At -> {
                        if (!enderman.isAngry()) {
                            Rotations.rotate(Rotations.getYaw(enderman), Rotations.getPitch(enderman, Target.Head), -75);
                        }
                    }
                }
            }
        }
    }

    /**
     * @see LivingEntity#isEntityLookingAtMe(LivingEntity, double, boolean, boolean, double...)
     */
    private boolean playerIsLookingAt(EndermanEntity entity) {
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
