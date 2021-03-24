/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.FluidCollisionShapeEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.entity.EntityUtils;
import net.minecraft.block.Material;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Jesus extends Module {
    private final SettingGroup sgWater = settings.createGroup("Water");
    private final SettingGroup sgLava = settings.createGroup("Lava");

    // Water

    private final Setting<Boolean> walkOnWater = sgWater.add(new BoolSetting.Builder()
            .name("walk-on-water")
            .description("Lets you walk on water.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnSneakForWater = sgWater.add(new BoolSetting.Builder()
            .name("disable-on-sneak-for-water")
            .description("Lets you go under the water when your sneak key is held.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> dipIntoWater = sgWater.add(new BoolSetting.Builder()
            .name("dip-into-water")
            .description("Lets you go under the water when you fall over a certain height.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> dipIntoWaterHeight = sgWater.add(new IntSetting.Builder()
            .name("dip-into-water-height")
            .description("Maximum safe height.")
            .defaultValue(4)
            .min(1)
            .max(255)
            .sliderMin(3)
            .sliderMax(21)
            .build()
    );

    private final Setting<Boolean> dipIntoWaterIfBurning = sgWater.add(new BoolSetting.Builder()
            .name("dip-into-water-if-burning")
            .description("Lets you go under the water when you are burning.")
            .defaultValue(true)
            .build()
    );

    // Lava

    private final Setting<Boolean> walkOnLava = sgLava.add(new BoolSetting.Builder()
            .name("walk-on-lava")
            .description("Lets you walk on lava.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnSneakForLava = sgLava.add(new BoolSetting.Builder()
            .name("disable-on-sneak-for-lava")
            .description("Lets you go under the lava when your sneak key is held.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> dipIntoLava = sgLava.add(new BoolSetting.Builder()
            .name("dip-into-lava")
            .description("Lets you go under the lava when you fall over than certain height.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> dipIntoLavaHeight = sgLava.add(new IntSetting.Builder()
            .name("dip-into-lava-height")
            .description("Maximum safe height.")
            .defaultValue(15)
            .min(1)
            .max(255)
            .sliderMin(3)
            .sliderMax(21)
            .build()
    );

    private final Setting<Boolean> dipIntoLavaIfFireResistance = sgLava.add(new BoolSetting.Builder()
            .name("dip-if-fire-resistance")
            .description("Lets you go under the lava if you have Fire Resistance effect.") // rofl some retard put "fall damage" here
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fireResistanceSafeMode = sgLava.add(new BoolSetting.Builder()
            .name("fire-resistance-safe-mode")
            .description("Prevents being in lava when the Fire Resistance effect is nearly over.")
            .defaultValue(true)
            .build()
    );
// make it so that you can customize the amount of time the effect has left for this to work if that makes sense.
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private int tickTimer = 10;
    private int packetTimer = 0;

    private boolean preBaritoneAssumeWalkOnWater;
    private boolean preBaritoneAssumeWalkOnLava;

    public Jesus() {
        super(Categories.Movement, "jesus", "Walk on liquids like Jesus.");
    }

    @Override
    public void onActivate() {
        preBaritoneAssumeWalkOnWater = BaritoneAPI.getSettings().assumeWalkOnWater.value;
        preBaritoneAssumeWalkOnLava = BaritoneAPI.getSettings().assumeWalkOnLava.value;

        BaritoneAPI.getSettings().assumeWalkOnWater.value = walkOnWater.get();
        BaritoneAPI.getSettings().assumeWalkOnLava.value = walkOnLava.get();
    }

    @Override
    public void onDeactivate() {
        BaritoneAPI.getSettings().assumeWalkOnWater.value = preBaritoneAssumeWalkOnWater;
        BaritoneAPI.getSettings().assumeWalkOnLava.value = preBaritoneAssumeWalkOnLava;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player.isTouchingWater() && !waterShouldBeSolid()) return;
        if (mc.player.isInLava() && !lavaShouldBeSolid()) return;

        // Move up in water
        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            Vec3d velocity = mc.player.getVelocity();
            ((IVec3d) velocity).set(velocity.x, 0.11, velocity.z);
            tickTimer = 0;
            return;
        }

        // Simulate jumping out of water
        Vec3d velocity = mc.player.getVelocity();
        if (tickTimer == 0)
            ((IVec3d) velocity).set(velocity.x, 0.30, velocity.z);
        else if (tickTimer == 1)
            ((IVec3d) velocity).set(velocity.x, 0, velocity.z);

        tickTimer++;
    }

    @EventHandler
    private void onCanWalkOnFluid(CanWalkOnFluidEvent event) {
        if (event.entity != mc.player) return;

        if ((event.fluid == Fluids.WATER || event.fluid == Fluids.FLOWING_WATER) && waterShouldBeSolid())
            event.walkOnFluid = true;
        else if ((event.fluid == Fluids.LAVA || event.fluid == Fluids.FLOWING_LAVA) && lavaShouldBeSolid())
            event.walkOnFluid = true;
    }

    @EventHandler
    private void onFluidCollisionShape(FluidCollisionShapeEvent event) {
        if (event.state.getMaterial() == Material.WATER && !mc.player.isTouchingWater() && waterShouldBeSolid())
            event.shape = VoxelShapes.fullCube();
        else if (event.state.getMaterial() == Material.LAVA && !mc.player.isInLava() && lavaShouldBeSolid())
            event.shape = VoxelShapes.fullCube();
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerMoveC2SPacket)) return;
        if (mc.player.isTouchingWater() && !waterShouldBeSolid()) return;
        if (mc.player.isInLava() && !lavaShouldBeSolid()) return;

        PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.packet;

        // Check if packet contains a position
        if (!(packet instanceof PlayerMoveC2SPacket.PositionOnly || packet instanceof PlayerMoveC2SPacket.Both)) return;

        // Check inWater, fallDistance and if over liquid
        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.fallDistance > 3f || !isOverLiquid()) return;

        // If not actually moving, cancel packet
        if (mc.player.input.movementForward == 0 && mc.player.input.movementSideways == 0) {
            event.cancel();
            return;
        }

        // Wait for timer
        if (packetTimer++ < 4) return;
        packetTimer = 0;

        // Cancel old packet
        event.cancel();

        // Get position
        double x = packet.getX(0);
        double y = packet.getY(0) + 0.05;
        double z = packet.getZ(0);

        // Create new packet
        Packet<?> newPacket;
        if (packet instanceof PlayerMoveC2SPacket.PositionOnly)
            newPacket = new PlayerMoveC2SPacket.PositionOnly(x, y, z, true);
        else
            newPacket = new PlayerMoveC2SPacket.Both(x, y, z, packet.getYaw(0), packet.getPitch(0), true);

        // Send new packet
        mc.getNetworkHandler().getConnection().send(newPacket);
    }

    private boolean waterShouldBeSolid() {
        return walkOnWater.get() &&
                !(disableOnSneakForWater.get() && mc.options.keySneak.isPressed()) &&
                !(dipIntoWater.get() && mc.player.fallDistance > dipIntoWaterHeight.get()) &&
                !(dipIntoWaterIfBurning.get() && mc.player.isOnFire()) &&
                !(EntityUtils.getGameMode(mc.player) == GameMode.SPECTATOR) &&
                !(mc.player.abilities.flying);
    }

    private boolean lavaIsSafe() {
        if (!dipIntoLavaIfFireResistance.get()) return false;

        return mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) &&
                (!fireResistanceSafeMode.get() || mc.player.getStatusEffect(StatusEffects.FIRE_RESISTANCE).getDuration() > ProtectionEnchantment.transformFireDuration(mc.player, 15 * 20));
    }

    private boolean lavaShouldBeSolid() {
        return walkOnLava.get() &&
                !((disableOnSneakForLava.get() || lavaIsSafe()) && mc.options.keySneak.isPressed()) &&
                !(dipIntoLava.get() && mc.player.fallDistance > dipIntoLavaHeight.get()) &&
                !(lavaIsSafe() && mc.player.fallDistance > 3) &&
                !(EntityUtils.getGameMode(mc.player) == GameMode.SPECTATOR) &&
                !(mc.player.abilities.flying);
    }

    private boolean isOverLiquid() {
        boolean foundLiquid = false;
        boolean foundSolid = false;

        List<Box> blockCollisions = mc.world
                .getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0, -0.5, 0))
                .map(VoxelShape::getBoundingBox)
                .collect(Collectors.toCollection(ArrayList::new));

        for (Box bb : blockCollisions) {
            blockPos.set(MathHelper.lerp(0.5D, bb.minX, bb.maxX), MathHelper.lerp(0.5D, bb.minY, bb.maxY), MathHelper.lerp(0.5D, bb.minZ, bb.maxZ));
            Material material = mc.world.getBlockState(blockPos).getMaterial();

            if (material == Material.WATER || material == Material.LAVA)
                foundLiquid = true;
            else if (material != Material.AIR)
                foundSolid = true;
        }

        return foundLiquid && !foundSolid;
    }
}
