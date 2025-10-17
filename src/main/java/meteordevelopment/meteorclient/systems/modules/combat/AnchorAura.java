/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AnchorAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("Range in which to target players.")
        .defaultValue(10)
        .min(0)
        .sliderMax(16)
        .build()
    );

    private final Setting<SortPriority> targetPriority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("The minimum damage to inflict on your target.")
        .defaultValue(7)
        .min(0)
        .sliderMax(36)
        .build()
    );

    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-self-damage")
        .description("The maximum damage to inflict on yourself.")
        .defaultValue(7)
        .min(0)
        .sliderMax(36)
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Will not place and break anchors if they will kill you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switches to your previous slot after using anchors.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates server-side towards the anchors being placed/broken.")
        .defaultValue(true)
        .build()
    );

    // Place

    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
        .name("place")
        .description("Allows Anchor Aura to place anchors.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The tick delay between placing anchors.")
        .defaultValue(5)
        .range(0, 10)
        .visible(place::get)
        .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The range at which anchors can be placed.")
        .defaultValue(4)
        .range(0, 6)
        .visible(place::get)
        .build()
    );

    private final Setting<Double> placeWallsRange = sgPlace.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Range in which to place anchors when behind blocks.")
        .defaultValue(4)
        .range(0, 6)
        .visible(place::get)
        .build()
    );

    private final Setting<Boolean> airPlace = sgPlace.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Allows Anchor Aura to place anchors in the air.")
        .defaultValue(true)
        .visible(place::get)
        .build()
    );

    // Break

    private final Setting<Integer> chargeDelay = sgBreak.add(new IntSetting.Builder()
        .name("charge-delay")
        .description("The tick delay it takes to charge anchors.")
        .defaultValue(1)
        .range(0, 10)
        .build()
    );

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The tick delay it takes to break anchors.")
        .defaultValue(1)
        .range(0, 10)
        .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("Range in which to break anchors.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Range in which to break anchors when behind blocks.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
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
        .description("Whether to swing your hand client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the block where it is placing an anchor.")
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
    private final BlockPos.Mutable bestPlacePos = new BlockPos.Mutable();

    private double bestBreakDamage;
    private final BlockPos.Mutable bestBreakPos = new BlockPos.Mutable();

    private BlockPos renderBlockPos;
    private int placeDelayLeft, chargeDelayLeft, breakDelayLeft;
    private PlayerEntity target;

    public AnchorAura() {
        super(Categories.Combat, "anchor-aura", "Automatically places and breaks Respawn Anchors to harm entities.");
    }

    @Override
    public void onActivate() {
        renderBlockPos = null;
        placeDelayLeft = placeDelay.get();
        chargeDelayLeft = 0;
        breakDelayLeft = 0;
        target = null;
    }

    @Override
    public void onDeactivate() {
        renderBlockPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world.getDimension().respawnAnchorWorks()) {
            error("You are in the Nether... disabling.");
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

        doAnchorAura();
    }

    private void doAnchorAura() {
        bestPlaceDamage = 0;
        bestBreakDamage = 0;

        // Find best positions to place new anchors or break existing anchors
        int iteratorRange = (int) Math.ceil(Math.max(placeRange.get(), breakRange.get()));
        BlockIterator.register(iteratorRange, iteratorRange, (blockPos, blockState) -> {
            boolean isPlacing = blockState.getBlock() != Blocks.RESPAWN_ANCHOR;

            // Check raycast and range
            double baseRange = isPlacing ? placeRange.get() : breakRange.get();
            double wallsRange = isPlacing ? placeWallsRange.get() : breakWallsRange.get();
            if (isOutOfRange(blockPos, baseRange, wallsRange)) return;

            // Check placement requirements
            if (isPlacing) {
                if (!BlockUtils.canPlace(blockPos)) return;

                if (!airPlace.get() && isAirPlace(blockPos)) return;
            }

            float bestDamage = isPlacing ? (float) bestPlaceDamage : (float) bestBreakDamage;
            float selfDamage = DamageUtils.anchorDamage(mc.player, blockPos.toCenterPos());
            float targetDamage = DamageUtils.anchorDamage(target, blockPos.toCenterPos());

            // Is the anchor optimal?
            if (targetDamage >= minDamage.get() && targetDamage > bestDamage
                && (!antiSuicide.get() || selfDamage <= maxSelfDamage.get())
                && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - selfDamage > 0)) {

                if (isPlacing) {
                    bestPlaceDamage = targetDamage;
                    bestPlacePos.set(blockPos);
                } else {
                    bestBreakDamage = targetDamage;
                    bestBreakPos.set(blockPos);
                }
            }
        });

        BlockIterator.after(() -> {
            renderBlockPos = null;

            FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
            FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);

            if (bestBreakDamage > 0) {
                doBreak(glowStone);
            } else if (bestPlaceDamage > 0 && place.get() && anchor.found() && glowStone.found()) {
                doPlace(anchor);
            }
        });
    }

    private void doPlace(FindItemResult anchor) {
        // Set render info
        renderBlockPos = bestPlacePos;

        if (placeDelayLeft++ < placeDelay.get()) return;

        // Place anchor!
        BlockUtils.place(bestPlacePos, anchor, rotate.get(), 50, swing.get(), false, swapBack.get());

        placeDelayLeft = 0;
    }

    private void doBreak(FindItemResult glowStone) {
        // Set render info
        renderBlockPos = bestBreakPos;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(bestBreakPos), Rotations.getPitch(bestBreakPos), 40, () -> doInteract(glowStone));
        } else {
            doInteract(glowStone);
        }
    }

    private void doInteract(FindItemResult glowStone) {
        BlockState blockState = mc.world.getBlockState(bestBreakPos);
        if (blockState.getBlock() != Blocks.RESPAWN_ANCHOR) return;

        Vec3d center = bestBreakPos.toCenterPos();
        int charges = blockState.get(Properties.CHARGES);

        // Charge the anchor
        if (charges == 0 && chargeDelayLeft++ >= chargeDelay.get()) {
            if (!glowStone.found()) return;

            InvUtils.swap(glowStone.slot(), swapBack.get());
            BlockUtils.interact(new BlockHitResult(center, BlockUtils.getDirection(bestBreakPos), bestBreakPos, true), Hand.MAIN_HAND, swing.get());
            chargeDelayLeft = 0;
            charges++;
        }

        // Explode the anchor when charged
        if (charges > 0 && breakDelayLeft++ >= breakDelay.get()) {
            FindItemResult fir = InvUtils.findInHotbar(item -> !item.getItem().equals(Items.GLOWSTONE));
            if (!fir.found()) return;

            InvUtils.swap(fir.slot(), swapBack.get());
            BlockUtils.interact(new BlockHitResult(center, BlockUtils.getDirection(bestBreakPos), bestBreakPos, true), Hand.MAIN_HAND, swing.get());
            breakDelayLeft = 0;

            // Instantly break the anchor on client, stops invalid block placements
            mc.world.setBlockState(bestBreakPos, mc.world.getFluidState(bestBreakPos).getBlockState(), 0);
        }

        if (swapBack.get()) InvUtils.swapBack();
    }

    private boolean isOutOfRange(BlockPos blockPos, double baseRange, double wallsRange) {
        Vec3d pos = blockPos.toCenterPos();
        if (!PlayerUtils.isWithin(pos, baseRange)) return true;

        RaycastContext raycastContext = new RaycastContext(mc.player.getEyePos(), pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(raycastContext);
        if (result == null || !result.getBlockPos().equals(blockPos))
            return !PlayerUtils.isWithin(pos, wallsRange);

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
        return pauseOnCA.get() && CA.isActive() && CA.kaTimer > 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || renderBlockPos == null) return;

        event.renderer.box(renderBlockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}
