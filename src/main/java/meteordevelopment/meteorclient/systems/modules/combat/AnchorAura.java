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
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

import org.jetbrains.annotations.Nullable;
import com.google.common.util.concurrent.AtomicDouble;
import java.util.concurrent.atomic.AtomicReference;

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
        .range(0, 36)
        .build()
    );

    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-self-damage")
        .description("The maximum damage to inflict on yourself.")
        .defaultValue(7)
        .range(0, 36)
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Will not place and break anchors if they will kill you.")
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

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The tick delay between breaking anchors.")
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
        .description("Whether to swing hand client-side.")
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

    private BlockPos renderBlockPos;
    private int placeDelayLeft, breakDelayLeft;
    private PlayerEntity target;

    public AnchorAura() {
        super(Categories.Combat, "anchor-aura", "Automatically places and breaks Respawn Anchors to harm entities.");
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
    private void onTick(TickEvent.Post event) {
        if (mc.world.getDimension().respawnAnchorWorks()) {
            error("You are in the Nether... disabling.");
            toggle();
            return;
        }

        // Pause
        if (shouldPause()) return;

        // Find a target
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            renderBlockPos = null;
            target = TargetUtils.getPlayerTarget(targetRange.get(), targetPriority.get());
            if (TargetUtils.isBadTarget(target, targetRange.get())) return;
        }

        doAnchorAura();
    }

    private void doAnchorAura() {
        AtomicDouble bestPlaceDamage = new AtomicDouble(0);
        AtomicReference<BlockPos.Mutable> bestPlaceBlockPos = new AtomicReference<>(new BlockPos.Mutable());

        AtomicDouble bestBreakDamage = new AtomicDouble(0);
        AtomicReference<BlockPos.Mutable> bestBreakBlockPos = new AtomicReference<>(new BlockPos.Mutable());

        // Find best positions to place new anchors or break existing anchors
        int iterationSize = (int) Math.ceil(Math.max(placeRange.get(), breakRange.get()));
        BlockIterator.register(iterationSize, iterationSize, (blockPos, blockState) -> {
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

            float bestDamage = isPlacing ? bestPlaceDamage.floatValue() : bestBreakDamage.floatValue();
            float selfDamage = DamageUtils.anchorDamage(mc.player, blockPos.toCenterPos());
            float targetDamage = DamageUtils.anchorDamage(target, blockPos.toCenterPos());

            // Is the anchor optimal?
            if (targetDamage >= minDamage.get() && targetDamage > bestDamage
                && (!antiSuicide.get() || selfDamage <= maxSelfDamage.get())
                && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - selfDamage > 0)) {

                if (isPlacing) {
                    bestPlaceDamage.set(targetDamage);
                    bestPlaceBlockPos.get().set(blockPos);
                } else {
                    bestBreakDamage.set(targetDamage);
                    bestBreakBlockPos.get().set(blockPos);
                }
            }
        });

        BlockIterator.after(() -> {
            renderBlockPos = null;

            FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
            FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
            if (!anchor.found() || !glowStone.found()) return;

            if (bestPlaceDamage.get() > 0 && place.get()) {
                doPlace(bestPlaceBlockPos.get(), anchor);
            }

            if (bestBreakDamage.get() > 0) {
                doBreak(bestBreakBlockPos.get(), anchor, glowStone);
            }

            placeDelayLeft++;
        });
    }

    private void doPlace(BlockPos blockPos, FindItemResult anchor) {
        // Set render info
        renderBlockPos = blockPos;

        if (placeDelayLeft < placeDelay.get()) return;

        // Place anchor!
        BlockUtils.place(blockPos, anchor, rotate.get(), 50, swing.get());

        placeDelayLeft = 0;
    }

    private void doBreak(BlockPos blockPos, FindItemResult anchor, FindItemResult glowStone) {
        // Set render info
        renderBlockPos = blockPos;

        if (breakDelayLeft++ < breakDelay.get()) return;

        // Stop sneaking so interactions with the anchor are successful
        if (mc.player.isSneaking()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
        }

        Vec3d center = blockPos.toCenterPos();

        // Charge the anchor
        if (mc.world.getBlockState(blockPos).get(Properties.CHARGES) == 0) {
            InvUtils.swap(glowStone.slot(), true);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, Direction.UP, blockPos, true));
        }

        // Explode the anchor when charged
        if (mc.world.getBlockState(blockPos).get(Properties.CHARGES) > 0) {
            InvUtils.swap(anchor.slot(), true);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, Direction.UP, blockPos, true));
        }

        InvUtils.swapBack();

        breakDelayLeft = 0;
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
        if (pauseOnCA.get() && CA.isActive() && CA.kaTimer > 0) return true;

        return false;
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
