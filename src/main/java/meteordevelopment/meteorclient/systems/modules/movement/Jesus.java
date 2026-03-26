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
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
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

        if (mc.player.isVisuallySwimming()) return;
        if (mc.player.isInWater() && !waterShouldBeSolid()) return;
        if (mc.player.isInLava() && !lavaShouldBeSolid()) return;

        Entity movingEntity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;

        // Move up in bubble columns
        if (bubbleColumn) {
            if (mc.options.keyJump.isDown() && movingEntity.getDeltaMovement().y() < 0.11) ((IVec3d) movingEntity.getDeltaMovement()).meteor$setY(0.11);
            return;
        }

        // Move up
        if (movingEntity.isInWater() || movingEntity.isInLava()) {
            ((IVec3d) movingEntity.getDeltaMovement()).meteor$setY(0.11);
            ascending = 0;
            return;
        }

        BlockState blockBelowState = mc.level.getBlockState(movingEntity.blockPosition().below());
        boolean waterLogged = blockBelowState.getValueOrElse(BlockStateProperties.WATERLOGGED, false);

        if (ascending == 0) ((IVec3d) movingEntity.getDeltaMovement()).meteor$setY(0.11);
        else if (ascending == 1 && (blockBelowState.getBlock() == Blocks.WATER || blockBelowState.getBlock() == Blocks.LAVA || waterLogged))
            ((IVec3d) movingEntity.getDeltaMovement()).meteor$setY(0);

        ascending++;
    }

    @EventHandler
    private void onCanWalkOnFluid(CanWalkOnFluidEvent event) {
        if (mc.player != null && mc.player.isSwimming()) return;

        if ((event.fluidState.getType() == Fluids.WATER || event.fluidState.getType() == Fluids.FLOWING_WATER) && waterShouldBeSolid()) {
            event.walkOnFluid = true;
        }
        else if ((event.fluidState.getType() == Fluids.LAVA || event.fluidState.getType() == Fluids.FLOWING_LAVA) && lavaShouldBeSolid()) {
            event.walkOnFluid = true;
        }
    }

    @EventHandler
    private void onFluidCollisionShape(CollisionShapeEvent event) {
        if (event.state.getFluidState().isEmpty()) return;

        if ((event.state.getBlock() == Blocks.WATER || event.state.getFluidState().getType() == Fluids.WATER) && !mc.player.isInWater() && waterShouldBeSolid() && event.pos.getY() <= mc.player.getY() - 1) {
            event.shape = Shapes.block();
        } else if (event.state.getBlock() == Blocks.LAVA && !mc.player.isInLava() && lavaShouldBeSolid() && (isLavaDangerous() || event.pos.getY() <= mc.player.getY() - 1)) {
            event.shape = Shapes.block();
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundMovePlayerPacket packet)) return;
        if (mc.player.isInWater() && !waterShouldBeSolid()) return;
        if (mc.player.isInLava() && !lavaShouldBeSolid()) return;
        if (!ncpBypass.get()) return;

        // Check inWater, fallDistance and if over liquid
        Tuple<Boolean, Boolean> overLiquid = isOverLiquid();
        boolean shouldWork = (overLiquid.getA() && waterShouldBeSolid()) || (overLiquid.getB() && lavaShouldBeSolid());

        if (mc.player.isInWater() || mc.player.isInLava() || mc.player.fallDistance > 3f || !shouldWork) return;

        ((PlayerMoveC2SPacketAccessor) packet).meteor$setOnGround(false);
        if (!mc.player.onGround() || !packet.hasPosition()) return;

        ((PlayerMoveC2SPacketAccessor) packet).meteor$setY(packet.getY(0) - (0.02 + (0.0001 * swimmingTicks)));
    }

    @EventHandler
    private void onMoveEvent(PlayerMoveEvent event) {
        if (!ncpBypass.get()) return;

        Tuple<Boolean, Boolean> overLiquid = isOverLiquid();
        boolean water = overLiquid.getA() && waterShouldBeSolid();
        boolean lava  = overLiquid.getB() && lavaShouldBeSolid();

        if (!water && !lava) {
            swimmingTicks = 0;
            return;
        }

        if (++swimmingTicks < 15) {
            if (mc.player.onGround()) {
                Vector2d vel = Strafe.transformStrafe(PlayerUtils.isMoving() ? 0.2873 : 0);
                ((IVec3d) event.movement).meteor$setXZ(vel.x, vel.y);
            }

            return;
        }

        swimmingTicks = 0;
        if (slowDown.get()) ((IVec3d) event.movement).meteor$setXZ(0, 0);
        ((IVec3d) mc.player.getDeltaMovement()).meteor$setY(0.08);
    }

    private boolean waterShouldBeSolid() {
        if (EntityUtils.getGameMode(mc.player) == GameType.SPECTATOR || mc.player.getAbilities().flying) return false;

        if (mc.player.getVehicle() != null) {
            if (mc.player.getVehicle() instanceof AbstractBoat) return false;
        }

        if (Modules.get().get(Flight.class).isActive()) return false;

        if (dipIfBurning.get() && mc.player.isOnFire()) return false;
        if (dipOnSneakWater.get() && mc.options.keyShift.isDown()) return false;
        if (dipOnFallWater.get() && mc.player.fallDistance > dipFallHeightWater.get()) return false;

        return waterMode.get() == Mode.Solid;
    }

    private boolean lavaShouldBeSolid() {
        if (EntityUtils.getGameMode(mc.player) == GameType.SPECTATOR || mc.player.getAbilities().flying) return false;

        if (mc.player.getVehicle() != null) {
            if (mc.player.getVehicle() instanceof Strider) return false;
        }

        if (isLavaDangerous() && lavaMode.get() == Mode.Solid) return true;

        if (dipOnSneakLava.get() && mc.options.keyShift.isDown()) return false;
        if (dipOnFallLava.get() && mc.player.fallDistance > dipFallHeightLava.get()) return false;

        return lavaMode.get() == Mode.Solid;
    }

    private boolean isLavaDangerous() {
        if (!dipIfFireResistant.get()) return true;
        return !mc.player.hasEffect(MobEffects.FIRE_RESISTANCE) || (!(mc.player.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() > (15 * 20 * mc.player.getAttributeValue(Attributes.BURNING_TIME))));
    }

    private Tuple<Boolean, Boolean> isOverLiquid() {
        AABB box = mc.player.isPassenger() ? mc.player.getBoundingBox().minmax(mc.player.getVehicle().getBoundingBox()) : mc.player.getBoundingBox();
        BlockState[] states = mc.level.getBlockStates(box.move(0.0, -0.01, 0.0)).toArray(BlockState[]::new);

        boolean water = false, lava = false;
        boolean foundSolid = false;

        for (BlockState state : states) {
            if (state.getBlock() == Blocks.WATER || state.getFluidState().getType() == Fluids.WATER) water = true;
            else if (state.getBlock() == Blocks.LAVA) lava = true;

            else if (!state.isAir()) {
                foundSolid = true;
                break;
            }
        }

        return new Tuple<>(water && !foundSolid, lava && !foundSolid);
    }

    public enum Mode {
        Solid,
        Ignore
    }

    public boolean canWalkOnPowderSnow() {
        return isActive() && powderSnow.get();
    }
}
