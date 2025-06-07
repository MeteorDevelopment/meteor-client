/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.BedBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.item.BedItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class BedAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgAutoMove = settings.createGroup("Inventory");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
        .name("place")
        .description("Allows Bed Aura to place beds.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The range at which beds can be placed.")
        .defaultValue(4)
        .range(0, 6)
        .visible(place::get)
        .build()
    );

    private final Setting<Double> placeWallsRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Range in which to place beds when behind blocks.")
        .defaultValue(4)
        .range(0, 6)
        .visible(place::get)
        .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Allows Bed Aura to place beds in the air.")
        .defaultValue(true)
        .visible(place::get)
        .build()
    );

    private final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder()
        .name("strict-direction")
        .description("Only places beds in the direction you are facing.")
        .defaultValue(false)
        .visible(place::get)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The delay between placing beds in ticks.")
        .defaultValue(10)
        .range(0, 10)
        .visible(place::get)
        .build()
    );

    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The tick delay between exploding beds.")
        .defaultValue(2)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> spoofPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-place")
        .description("Places the entire bed synchronically on your client.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> spoofBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("spoof-break")
        .description("Breaks the entire bed synchronically on your client.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates server-side towards the beds being broken.")
        .defaultValue(true)
        .build()
    );

    // Targeting

    private final Setting<SortPriority> targetPriority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Double> targetRange = sgTargeting.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("Range in which to target players.")
        .defaultValue(10)
        .min(0)
        .sliderMax(16)
        .build()
    );

    private final Setting<Double> minDamage = sgTargeting.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("The minimum damage to inflict on your target.")
        .defaultValue(7)
        .range(0, 36)
        .build()
    );

    private final Setting<Double> maxSelfDamage = sgTargeting.add(new DoubleSetting.Builder()
        .name("max-self-damage")
        .description("The maximum damage to inflict on yourself.")
        .defaultValue(7)
        .range(0, 36)
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgTargeting.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Will not place and break beds if they will kill you.")
        .defaultValue(true)
        .build()
    );

    // Inventory

    private final Setting<Boolean> autoMove = sgAutoMove.add(new BoolSetting.Builder()
        .name("auto-move")
        .description("Moves beds into a selected hotbar slot.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> autoMoveSlot = sgAutoMove.add(new IntSetting.Builder()
        .name("auto-move-slot")
        .description("The slot auto move moves beds to.")
        .defaultValue(9)
        .range(1, 9)
        .sliderRange(1, 9)
        .visible(autoMove::get)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgAutoMove.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches to beds automatically.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgAutoMove.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switches to your previous slot after using beds.")
        .defaultValue(true)
        .visible(autoSwitch::get)
        .build()
    );

    // Pause

    private final Setting<Boolean> pauseOnUse = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("Pauses while using an item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses while mining blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnCA = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-CA")
        .description("Pauses while Crystal Aura is placing.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Whether to swing hand client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the block where it is placing a bed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color for positions to be placed.")
        .defaultValue(new SettingColor(15, 255, 211, 41))
        .visible(() -> render.get() && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color for positions to be placed.")
        .defaultValue(new SettingColor(15, 255, 211))
        .visible(() -> render.get() && shapeMode.get().lines())
        .build()
    );

    private double bestPlaceDamage;
    private BlockPos.Mutable bestPlacePos = new BlockPos.Mutable();
    private Direction bestPlaceDirection;

    private double bestBreakDamage;
    private BlockPos.Mutable bestBreakPos = new BlockPos.Mutable();

    private BlockPos renderBlockPos;
    private Direction renderDirection;
    private int placeDelayLeft, breakDelayLeft;
    private PlayerEntity target;

    public BedAura() {
        super(Categories.Combat, "bed-aura", "Automatically places and explodes beds in the Nether and End.");
    }

    @Override
    public void onActivate() {
        renderBlockPos = null;
        placeDelayLeft = placeDelay.get();
        breakDelayLeft = 0;
        target = null;
    }

    @Override
    public void onDeactivate() {
        renderBlockPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Check if beds can explode here
        if (mc.world.getDimension().bedWorks()) {
            error("You can't blow up beds in this dimension, disabling.");
            toggle();
            return;
        }

        // Pause
        if (shouldPause()) {
            renderBlockPos = null;
            return;
        }

        // Find a target
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            renderBlockPos = null;
            target = TargetUtils.getPlayerTarget(targetRange.get(), targetPriority.get());
            if (TargetUtils.isBadTarget(target, targetRange.get())) return;
        }

        // Auto move
        if (autoMove.get()) {
            FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
            boolean alreadyHasBed = mc.player.getInventory().getStack(autoMoveSlot.get() - 1).getItem() instanceof BedItem;
            if (bed.found() && !bed.isHotbar() && !alreadyHasBed) {
                InvUtils.move().from(bed.slot()).toHotbar(autoMoveSlot.get() - 1);
            }
        }

        doBedAura();
    }

    private void doBedAura() {
        bestPlaceDamage = 0;
        bestBreakDamage = 0;

        // Find best position to place or break the bed
        int iteratorRange = (int) Math.ceil(placeRange.get());
        BlockIterator.register(iteratorRange, iteratorRange, (blockPos, blockState) -> {
            if (blockState.getBlock() instanceof BedBlock) {
                setBreakInfo(blockPos, blockState);
            } else if (place.get()) {
                setPlaceInfo(blockPos);
            }
        });

        // Break or place
        BlockIterator.after(() -> {
            renderBlockPos = null;

            if (bestBreakDamage > 0) {
                doBreak();
            } else if (bestPlaceDamage > 0) {
                doPlace();
            }
        });
    }

    private void setPlaceInfo(BlockPos footPos) {
        if (!BlockUtils.canPlace(footPos)) return;

        // Air place check
        if (!airPlace.get() && isAirPlace(footPos)) return;

        // Check raycast and range
        if (isOutOfRange(footPos)) return;

        Direction rotateDirection = Direction.fromHorizontalDegrees(Rotations.getYaw(footPos.toCenterPos()));

        for (Direction placeDirection : Direction.HORIZONTAL) {
            BlockPos headPos = footPos.offset(placeDirection);
            if (!mc.world.getBlockState(headPos).isReplaceable()) continue;

            // Match our player's horizontal facing if we are using strict direction
            if (strictDirection.get() && placeDirection != rotateDirection) continue;

            float targetDamage = DamageUtils.bedDamage(target, headPos.toCenterPos());
            if (isBestDamage(headPos, targetDamage, bestPlaceDamage)) {
                bestPlaceDamage = targetDamage;
                bestPlaceDirection = placeDirection;
                bestPlacePos.set(footPos);
            }
        }
    }

    private void setBreakInfo(BlockPos blockPos, BlockState blockState) {
        // Check raycast and range
        if (isOutOfRange(blockPos)) return;

        BlockPos otherPos = blockPos.offset(BedBlock.getOppositePartDirection(blockState));
        BlockPos headPos = blockState.get(BedBlock.PART) == BedPart.HEAD ? blockPos : otherPos;

        float targetDamage = DamageUtils.bedDamage(target, headPos.toCenterPos());
        if (isBestDamage(headPos, targetDamage, bestBreakDamage)) {
            bestBreakDamage = targetDamage;
            bestBreakPos.set(blockPos);
        }
    }

    private boolean isBestDamage(BlockPos headPos, float targetDamage, double bestDamage) {
        float selfDamage = DamageUtils.bedDamage(mc.player, headPos.toCenterPos());

        // Is the bed optimal?
        return targetDamage >= minDamage.get() && targetDamage > bestDamage
            && (!antiSuicide.get() || selfDamage <= maxSelfDamage.get())
            && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - selfDamage > 0);
    }

    private void doPlace() {
        FindItemResult bed = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!bed.found()) return;

        // Set render info
        renderBlockPos = bestPlacePos;
        renderDirection = bestPlaceDirection;

        if (placeDelayLeft++ < placeDelay.get()) return;

        if (autoSwitch.get()) InvUtils.swap(bed.slot(), swapBack.get());

        // Get rotation to use depending on what direction we are doing
        double yaw = Direction.getHorizontalDegreesOrThrow(bestPlaceDirection);

        // Use legit rotation if strict direction is enabled
        if (strictDirection.get()) yaw = Rotations.getYaw(bestPlacePos);

        // Place bed!
        Rotations.rotate(yaw, Rotations.getPitch(bestPlacePos), () -> {
            BlockUtils.place(bestPlacePos, bed, false, 0, swing.get(), true);
        });

        if (swapBack.get()) InvUtils.swapBack();

        placeDelayLeft = 0;
    }

    private void doBreak() {
        // Set render info
        renderBlockPos = bestBreakPos;
        renderDirection = BedBlock.getOppositePartDirection(mc.world.getBlockState(renderBlockPos));

        if (breakDelayLeft++ < breakDelay.get()) return;

        // Break bed!
        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(bestBreakPos), Rotations.getPitch(bestBreakPos), () -> { doInteract(); });
        } else {
            doInteract();
        }

        breakDelayLeft = 0;
    }

    private void doInteract() {
        // Stop sneaking so interactions with the bed are successful
        if (mc.player.isSneaking()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
        }

        BlockUtils.interact(new BlockHitResult(bestBreakPos.toCenterPos(), BlockUtils.getDirection(bestBreakPos), bestBreakPos, true), Hand.MAIN_HAND, swing.get());
    }

    private boolean isOutOfRange(BlockPos blockPos) {
        Vec3d pos = blockPos.toCenterPos();
        if (!PlayerUtils.isWithin(pos, placeRange.get())) return true;

        RaycastContext raycastContext = new RaycastContext(mc.player.getEyePos(), pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(raycastContext);
        if (result == null || !result.getBlockPos().equals(blockPos))
            return !PlayerUtils.isWithin(pos, placeWallsRange.get());

        return false;
    }

    private boolean isAirPlace(BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            if (!mc.world.getBlockState(blockPos.offset(direction)).isReplaceable()) return false;
        }
        return true;
    }

    private boolean shouldPause() {
        if (pauseOnUse.get() && mc.player.isUsingItem()) return true;

        if (pauseOnMine.get() && mc.interactionManager.isBreakingBlock()) return true;

        CrystalAura CA = Modules.get().get(CrystalAura.class);
        if (pauseOnCA.get() && CA.isActive() && CA.kaTimer > 0) return true;

        return false;
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        // Spoof placement for instantaneous bed sync
        if (spoofPlace.get() && event.newState.getBlock() instanceof BedBlock && event.oldState.isReplaceable()) {
            BlockPos otherPos = event.pos.offset(BedBlock.getOppositePartDirection(event.newState));
            if (mc.world.getBlockState(otherPos).isReplaceable()) {
                mc.world.setBlockState(otherPos, event.newState.with(BedBlock.PART, BedPart.HEAD), 0);
            }
        }
        // Spoof bed breaking for instantaneous bed sync
        if (spoofBreak.get() && event.oldState.getBlock() instanceof BedBlock && event.newState.isReplaceable()) {
            BlockPos otherPos = event.pos.offset(BedBlock.getOppositePartDirection(event.oldState));
            if (mc.world.getBlockState(otherPos).getBlock() instanceof BedBlock) {
                mc.world.setBlockState(otherPos, mc.world.getFluidState(otherPos).getBlockState(), 0);
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || renderBlockPos == null) return;

        int x = renderBlockPos.getX(), y = renderBlockPos.getY(), z = renderBlockPos.getZ();

        switch (renderDirection) {
            case Direction.SOUTH -> event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            case Direction.NORTH -> event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            case Direction.WEST -> event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            case Direction.EAST -> event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
