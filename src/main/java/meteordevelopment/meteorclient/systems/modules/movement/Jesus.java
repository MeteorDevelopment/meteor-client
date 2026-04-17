/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.EntityMixin;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.speed.modes.Strafe;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.GameMode;
import org.joml.Vector2d;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class Jesus extends Module {
    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgWater = settings.createGroup("Water");
    private final SettingGroup sgLava = settings.createGroup("Lava");

    // General

    private final Setting<Boolean> powderSnow = sgGeneral.add(new BoolSetting.Builder()
        .name("powder-snow")
        .description("Walk on powder snow.")
        .defaultValue(true)
        .build()
    );

    /**
     * @see <a href="https://github.com/lambda-client/lambda/blob/9490eec0e81faf33a318ac1cabbdeb4f4c4f6850/src/main/kotlin/com/lambda/module/modules/movement/Jesus.kt">"NCP New" mode</a>
     * Author: bladekt
     */
    private final Setting<Boolean> ncpBypass = sgGeneral.add(new BoolSetting.Builder()
        .name("ncp-bypass")
        .description("Whether to apply a bypass for NCP.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> slowDown = sgGeneral.add(new BoolSetting.Builder()
        .name("slow-down")
        .description("Further movement option to try bypassing NCP")
        .defaultValue(false)
        .visible(ncpBypass::get)
        .build()
    );

    // Water

    private final Setting<Mode> waterMode = sgWater.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How to treat the water.")
        .defaultValue(Mode.Solid)
        .build()
    );

    private final Setting<Boolean> dipIfBurning = sgWater.add(new BoolSetting.Builder()
        .name("dip-if-burning")
        .description("Lets you go into the water when you are burning.")
        .defaultValue(true)
        .visible(() -> waterMode.get() == Mode.Solid)
        .build()
    );

    private final Setting<Boolean> dipOnSneakWater = sgWater.add(new BoolSetting.Builder()
        .name("dip-on-sneak")
        .description("Lets you go into the water when your sneak key is held.")
        .defaultValue(true)
        .visible(() -> waterMode.get() == Mode.Solid)
        .build()
    );

    private final Setting<Boolean> dipOnFallWater = sgWater.add(new BoolSetting.Builder()
        .name("dip-on-fall")
        .description("Lets you go into the water when you fall over a certain height.")
        .defaultValue(true)
        .visible(() -> waterMode.get() == Mode.Solid)
        .build()
    );

    private final Setting<Integer> dipFallHeightWater = sgWater.add(new IntSetting.Builder()
        .name("dip-fall-height")
        .description("The fall height at which you will go into the water.")
        .defaultValue(4)
        .range(1, 255)
        .sliderRange(3, 20)
        .visible(() -> waterMode.get() == Mode.Solid && dipOnFallWater.get())
        .build()
    );

    // Lava

    private final Setting<Mode> lavaMode = sgLava.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How to treat the lava.")
        .defaultValue(Mode.Solid)
        .build()
    );

    private final Setting<Boolean> dipIfFireResistant = sgLava.add(new BoolSetting.Builder()
        .name("dip-if-resistant")
        .description("Lets you go into the lava if you have Fire Resistance effect.")
        .defaultValue(true)
        .visible(() -> lavaMode.get() == Mode.Solid)
        .build()
    );

    private final Setting<Boolean> dipOnSneakLava = sgLava.add(new BoolSetting.Builder()
        .name("dip-on-sneak")
        .description("Lets you go into the lava when your sneak key is held.")
        .defaultValue(true)
        .visible(() -> lavaMode.get() == Mode.Solid)
        .build()
    );

    private final Setting<Boolean> dipOnFallLava = sgLava.add(new BoolSetting.Builder()
        .name("dip-on-fall")
        .description("Lets you go into the lava when you fall over a certain height.")
        .defaultValue(true)
        .visible(() -> lavaMode.get() == Mode.Solid)
        .build()
    );

    private final Setting<Integer> dipFallHeightLava = sgLava.add(new IntSetting.Builder()
        .name("dip-fall-height")
        .description("The fall height at which you will go into the lava.")
        .defaultValue(4)
        .range(1, 255)
        .sliderRange(3, 20)
        .visible(() -> lavaMode.get() == Mode.Solid && dipOnFallLava.get())
        .build()
    );

    // Other

    private int ascending = 10;
    private int swimmingTicks = 0;

    private boolean prePathManagerWalkOnWater;
    private boolean prePathManagerWalkOnLava;

    /**
     * {@link EntityMixin#onBubbleColumnSurfaceCollision(CallbackInfo)}
     * {@link EntityMixin#onBubbleColumnCollision(CallbackInfo)}
     */
    public boolean isInBubbleColumn = false;

    public Jesus() {
        super(Categories.Movement, "jesus", "Walk on liquids and powder snow like Jesus.");
    }

    @Override
    public void onActivate() {
        prePathManagerWalkOnWater = PathManagers.get().getSettings().getWalkOnWater().get();
        prePathManagerWalkOnLava = PathManagers.get().getSettings().getWalkOnLava().get();

        PathManagers.get().getSettings().getWalkOnWater().set(waterMode.get() == Mode.Solid);
        PathManagers.get().getSettings().getWalkOnLava().set(lavaMode.get() == Mode.Solid);
    }

    @Override
    public void onDeactivate() {
        PathManagers.get().getSettings().getWalkOnWater().set(prePathManagerWalkOnWater);
        PathManagers.get().getSettings().getWalkOnLava().set(prePathManagerWalkOnLava);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean bubbleColumn = isInBubbleColumn;
        isInBubbleColumn = false;

        if (mc.player.isInSwimmingPose()) return;
        if (mc.player.isTouchingWater() && !waterShouldBeSolid()) return;
        if (mc.player.isInLava() && !lavaShouldBeSolid()) return;

        Entity movingEntity = mc.player.hasVehicle() ? mc.player.getVehicle() : mc.player;

        // Move up in bubble columns
        if (bubbleColumn) {
            if (mc.options.jumpKey.isPressed() && movingEntity.getVelocity().getY() < 0.11) ((IVec3d) movingEntity.getVelocity()).meteor$setY(0.11);
            return;
        }

        // Move up
        if (movingEntity.isTouchingWater() || movingEntity.isInLava()) {
            ((IVec3d) movingEntity.getVelocity()).meteor$setY(0.11);
            ascending = 0;
            return;
        }

        BlockState blockBelowState = mc.world.getBlockState(movingEntity.getBlockPos().down());
        boolean waterLogged = blockBelowState.get(Properties.WATERLOGGED, false);

        if (ascending == 0) ((IVec3d) movingEntity.getVelocity()).meteor$setY(0.11);
        else if (ascending == 1 && (blockBelowState.getBlock() == Blocks.WATER || blockBelowState.getBlock() == Blocks.LAVA || waterLogged))
            ((IVec3d) movingEntity.getVelocity()).meteor$setY(0);

        ascending++;
    }

    @EventHandler
    private void onCanWalkOnFluid(CanWalkOnFluidEvent event) {
        if (mc.player != null && mc.player.isSwimming()) return;

        if ((event.fluidState.getFluid() == Fluids.WATER || event.fluidState.getFluid() == Fluids.FLOWING_WATER) && waterShouldBeSolid()) {
            event.walkOnFluid = true;
        }
        else if ((event.fluidState.getFluid() == Fluids.LAVA || event.fluidState.getFluid() == Fluids.FLOWING_LAVA) && lavaShouldBeSolid()) {
            event.walkOnFluid = true;
        }
    }

    @EventHandler
    private void onFluidCollisionShape(CollisionShapeEvent event) {
        if (event.state.getFluidState().isEmpty()) return;

        if ((event.state.getBlock() == Blocks.WATER || event.state.getFluidState().getFluid() == Fluids.WATER) && !mc.player.isTouchingWater() && waterShouldBeSolid() && event.pos.getY() <= mc.player.getY() - 1) {
            event.shape = VoxelShapes.fullCube();
        } else if (event.state.getBlock() == Blocks.LAVA && !mc.player.isInLava() && lavaShouldBeSolid() && (isLavaDangerous() || event.pos.getY() <= mc.player.getY() - 1)) {
            event.shape = VoxelShapes.fullCube();
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerMoveC2SPacket packet)) return;
        if (mc.player.isTouchingWater() && !waterShouldBeSolid()) return;
        if (mc.player.isInLava() && !lavaShouldBeSolid()) return;
        if (!ncpBypass.get()) return;

        // Check inWater, fallDistance and if over liquid
        Pair<Boolean, Boolean> overLiquid = isOverLiquid();
        boolean shouldWork = (overLiquid.getLeft() && waterShouldBeSolid()) || (overLiquid.getRight() && lavaShouldBeSolid());

        if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.fallDistance > 3f || !shouldWork) return;

        ((PlayerMoveC2SPacketAccessor) packet).meteor$setOnGround(false);
        if (!mc.player.isOnGround() || !packet.changesPosition()) return;

        ((PlayerMoveC2SPacketAccessor) packet).meteor$setY(packet.getY(0) - (0.02 + (0.0001 * swimmingTicks)));
    }

    @EventHandler
    private void onMoveEvent(PlayerMoveEvent event) {
        if (!ncpBypass.get()) return;

        Pair<Boolean, Boolean> overLiquid = isOverLiquid();
        boolean water = overLiquid.getLeft() && waterShouldBeSolid();
        boolean lava  = overLiquid.getRight() && lavaShouldBeSolid();

        if (!water && !lava) {
            swimmingTicks = 0;
            return;
        }

        if (++swimmingTicks < 15) {
            if (mc.player.isOnGround()) {
                Vector2d vel = Strafe.transformStrafe(PlayerUtils.isMoving() ? 0.2873 : 0);
                ((IVec3d) event.movement).meteor$setXZ(vel.x, vel.y);
            }

            return;
        }

        swimmingTicks = 0;
        if (slowDown.get()) ((IVec3d) event.movement).meteor$setXZ(0, 0);
        ((IVec3d) mc.player.getVelocity()).meteor$setY(0.08);
    }

    private boolean waterShouldBeSolid() {
        if (EntityUtils.getGameMode(mc.player) == GameMode.SPECTATOR || mc.player.getAbilities().flying) return false;

        if (mc.player.getVehicle() != null) {
            if (mc.player.getVehicle() instanceof AbstractBoatEntity) return false;
        }

        if (Modules.get().get(Flight.class).isActive()) return false;

        if (dipIfBurning.get() && mc.player.isOnFire()) return false;
        if (dipOnSneakWater.get() && mc.options.sneakKey.isPressed()) return false;
        if (dipOnFallWater.get() && mc.player.fallDistance > dipFallHeightWater.get()) return false;

        return waterMode.get() == Mode.Solid;
    }

    private boolean lavaShouldBeSolid() {
        if (EntityUtils.getGameMode(mc.player) == GameMode.SPECTATOR || mc.player.getAbilities().flying) return false;

        if (mc.player.getVehicle() != null) {
            if (mc.player.getVehicle() instanceof StriderEntity) return false;
        }

        if (isLavaDangerous() && lavaMode.get() == Mode.Solid) return true;

        if (dipOnSneakLava.get() && mc.options.sneakKey.isPressed()) return false;
        if (dipOnFallLava.get() && mc.player.fallDistance > dipFallHeightLava.get()) return false;

        return lavaMode.get() == Mode.Solid;
    }

    private boolean isLavaDangerous() {
        if (!dipIfFireResistant.get()) return true;
        return !mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) || (!(mc.player.getStatusEffect(StatusEffects.FIRE_RESISTANCE).getDuration() > (15 * 20 * mc.player.getAttributeValue(EntityAttributes.BURNING_TIME))));
    }

    private Pair<Boolean, Boolean> isOverLiquid() {
        Box box = mc.player.hasVehicle() ? mc.player.getBoundingBox().union(mc.player.getVehicle().getBoundingBox()) : mc.player.getBoundingBox();
        BlockState[] states = mc.world.getStatesInBox(box.offset(0.0, -0.01, 0.0)).toArray(BlockState[]::new);

        boolean water = false, lava = false;
        boolean foundSolid = false;

        for (BlockState state : states) {
            if (state.getBlock() == Blocks.WATER || state.getFluidState().getFluid() == Fluids.WATER) water = true;
            else if (state.getBlock() == Blocks.LAVA) lava = true;

            else if (!state.isAir()) {
                foundSolid = true;
                break;
            }
        }

        return new Pair<>(water && !foundSolid, lava && !foundSolid);
    }

    public enum Mode {
        Solid,
        Ignore
    }

    public boolean canWalkOnPowderSnow() {
        return isActive() && powderSnow.get();
    }
}
