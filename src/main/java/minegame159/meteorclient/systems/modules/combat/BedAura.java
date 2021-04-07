/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.player.*;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BedAura extends Module {
    private enum Stage {
        Placing,
        Breaking
    }

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Place

    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
            .name("place")
            .description("Allows Bed Aura to place beds.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Safety> placeMode = sgPlace.add(new EnumSetting.Builder<Safety>()
            .name("place-mode")
            .description("The way beds are allowed to be placed near you.")
            .defaultValue(Safety.Safe)
            .build()
    );

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
            .name("place-delay")
            .description("The tick delay for placing beds.")
            .defaultValue(9)
            .min(0)
            .sliderMax(20)
            .build()
    );

    // Break

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
            .name("break-delay")
            .description("The tick delay for breaking beds.")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Safety> breakMode = sgBreak.add(new EnumSetting.Builder<Safety>()
            .name("break-mode")
            .description("The way beds are allowed to be broken near you.")
            .defaultValue(Safety.Safe)
            .build()
    );

    // Pause

    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses while eating.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses while drinking potions.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses while mining blocks.")
            .defaultValue(false)
            .build()
    );

    // Misc

    private final Setting<Double> targetRange = sgMisc.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range for players to be targeted.")
            .defaultValue(4)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgMisc.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to a bed automatically.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> swapBack = sgMisc.add(new BoolSetting.Builder()
            .name("swap-back")
            .description("Switches back to previous slot after placing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoMove = sgMisc.add(new BoolSetting.Builder()
            .name("auto-move")
            .description("Moves beds into a selected hotbar slot.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> autoMoveSlot = sgMisc.add(new IntSetting.Builder()
            .name("auto-move-slot")
            .description("The slot Auto Move moves beds to.")
            .defaultValue(9)
            .min(1)
            .sliderMin(1)
            .max(9)
            .sliderMax(9)
            .build()
    );

    private final Setting<Boolean> noSwing = sgMisc.add(new BoolSetting.Builder()
            .name("no-swing")
            .description("Disables hand swings clientside.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> minDamage = sgMisc.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage to inflict on your target.")
            .defaultValue(7)
            .min(0)
            .sliderMax(20)
            .max(20)
            .build()
    );

    private final Setting<Double> maxSelfDamage = sgMisc.add(new DoubleSetting.Builder()
            .name("max-self-damage")
            .description("The maximum damage to inflict on yourself.")
            .defaultValue(7)
            .min(0)
            .sliderMax(20)
            .max(20)
            .build()
    );

    private final Setting<Double> minHealth = sgMisc.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health required for Bed Aura to work.")
            .defaultValue(4)
            .min(0)
            .sliderMax(36)
            .max(36)
            .build()
    );

    private final Setting<SortPriority> priority = sgMisc.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("How to select the player to target.")
            .defaultValue(SortPriority.LowestHealth)
            .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders the block where it is placing a bed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("place-side-color")
            .description("The side color for positions to be placed.")
            .defaultValue(new SettingColor(0, 0, 0, 75))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("place-line-color")
            .description("The line color for positions to be placed.")
            .defaultValue(new SettingColor(15, 255, 211, 255))
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private Direction direction;
    private PlayerEntity target;
    private BlockPos bestPos;

    private int breakDelayLeft;
    private int placeDelayLeft;

    private Stage stage;

    public BedAura(){
        super(Categories.Combat, "bed-aura", "Automatically places and explodes beds in the Nether and End.");
    }

    @Override
    public void onActivate() {
        if (place.get()) stage = Stage.Placing;
        else stage = Stage.Breaking;

        bestPos = null;

        direction = Direction.EAST;

        placeDelayLeft = placeDelay.get();
        breakDelayLeft = placeDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world.getDimension().isBedWorking()) {
            ChatUtils.moduleError(this, "You are in the Overworld... disabling!");
            toggle();
            return;
        }

        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        if (EntityUtils.getTotalHealth(mc.player) <= minHealth.get()) return;

        target = EntityUtils.getPlayerTarget(targetRange.get(), priority.get(), false);

        if (target == null) {
            bestPos = null;
            return;
        }

        if (place.get() && InvUtils.findItemInAll(itemStack -> itemStack.getItem() instanceof BedItem) != -1) {
            switch (stage) {
                case Placing:
                    bestPos = getPlacePos(target);

                    if (placeDelayLeft > 0) placeDelayLeft--;
                    else {
                        placeBed(bestPos);
                        placeDelayLeft = placeDelay.get();
                        stage = Stage.Breaking;
                    }
                case Breaking:
                    bestPos = getBreakPos(target);

                    if (breakDelayLeft > 0) breakDelayLeft--;
                    else {
                        breakBed(bestPos);
                        breakDelayLeft = breakDelay.get();
                        stage = Stage.Placing;
                    }
            }
        } else {
            bestPos = getBreakPos(target);

            if (breakDelayLeft > 0) breakDelayLeft--;
            else {
                breakDelayLeft = breakDelay.get();
                breakBed(bestPos);
            }
        }
    }

    private void placeBed(BlockPos pos) {
        if (pos == null || InvUtils.findItemInAll(itemStack -> itemStack.getItem() instanceof BedItem) == -1) return;

        if (autoMove.get()) doAutoMove();

        int slot = InvUtils.findItemInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (slot == -1) return;

        if (autoSwitch.get()) mc.player.inventory.selectedSlot = slot;

        Hand hand = InvUtils.getHand(itemStack -> itemStack.getItem() instanceof BedItem);
        if (hand == null) return;

        Rotations.rotate(yawFromDir(direction), mc.player.pitch, () -> BlockUtils.place(pos, hand, slot, false, 100, !noSwing.get(), true, autoSwitch.get(), swapBack.get()));
    }

    private void breakBed(BlockPos pos) {
        if (pos == null) return;

        boolean wasSneaking = mc.player.isSneaking();
        if (wasSneaking) mc.player.input.sneaking = false;
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, bestPos, false));
        if (wasSneaking) mc.player.input.sneaking = true;
    }

    private BlockPos getPlacePos(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();

        if (checkPlace(Direction.NORTH, target, true)) return targetPos.up().north();
        if (checkPlace(Direction.SOUTH, target, true)) return targetPos.up().south();
        if (checkPlace(Direction.EAST, target, true)) return targetPos.up().east();
        if (checkPlace(Direction.WEST, target, true)) return targetPos.up().west();

        if (checkPlace(Direction.NORTH, target, false)) return targetPos.north();
        if (checkPlace(Direction.SOUTH, target, false)) return targetPos.south();
        if (checkPlace(Direction.EAST, target, false)) return targetPos.east();
        if (checkPlace(Direction.WEST, target, false)) return targetPos.west();

        return null;
    }

    private boolean checkPlace(Direction direction, PlayerEntity target, boolean up) {
        BlockPos headPos = up ? target.getBlockPos().up() : target.getBlockPos();

        if (mc.world.getBlockState(headPos).getMaterial().isReplaceable()
                && BlockUtils.canPlace(headPos.offset(direction))
                && (placeMode.get() == Safety.Suicide
                || (DamageCalcUtils.bedDamage(target, Utils.vec3d(headPos)) >= minDamage.get()
                && DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(headPos.offset(direction))) < maxSelfDamage.get()
                && DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(headPos)) < maxSelfDamage.get()))) {
            this.direction = direction;
            return true;
        }

        return false;
    }

    private BlockPos getBreakPos(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();

        if (checkBreak(Direction.NORTH, target, true)) return targetPos.up().north();
        if (checkBreak(Direction.SOUTH, target, true)) return targetPos.up().south();
        if (checkBreak(Direction.EAST, target, true)) return targetPos.up().east();
        if (checkBreak(Direction.WEST, target, true)) return targetPos.up().west();

        if (checkBreak(Direction.NORTH, target, false)) return targetPos.north();
        if (checkBreak(Direction.SOUTH, target, false)) return targetPos.south();
        if (checkBreak(Direction.EAST, target, false)) return targetPos.east();
        if (checkBreak(Direction.WEST, target, false)) return targetPos.west();

        return null;
    }

    private boolean checkBreak(Direction direction, PlayerEntity target, boolean up) {
        BlockPos headPos = up ? target.getBlockPos().up() : target.getBlockPos();

        if (mc.world.getBlockState(headPos).getBlock() instanceof BedBlock
                && mc.world.getBlockState(headPos.offset(direction)).getBlock() instanceof BedBlock
                && (breakMode.get() == Safety.Suicide
                || (DamageCalcUtils.bedDamage(target, Utils.vec3d(headPos)) >= minDamage.get()
                && DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(headPos.offset(direction))) < maxSelfDamage.get()
                && DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(headPos)) < maxSelfDamage.get()))) {
            this.direction = direction;
            return true;
        }
        return false;
    }

    private void doAutoMove() {
        if (InvUtils.findItemInHotbar(itemStack -> itemStack.getItem() instanceof BedItem) == -1) {
            int slot = InvUtils.findItemInMain(itemStack -> itemStack.getItem() instanceof BedItem);
            InvUtils.move().from(slot).toHotbar(autoMoveSlot.get() - 1);
        }
    }

    private float yawFromDir(Direction direction) {
        switch (direction) {
            case EAST:  return 90;
            case NORTH: return 0;
            case SOUTH: return 180;
            case WEST:  return -90;
        }
        return 0;
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (render.get() && bestPos != null) {
            int x = bestPos.getX();
            int y = bestPos.getY();
            int z = bestPos.getZ();

            switch (direction) {
                case NORTH:
                    Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z, x + 1, y + 0.6, z + 2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    break;
                case SOUTH:
                    Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z - 1, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    break;
                case EAST:
                    Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x - 1, y, z, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    break;
                case WEST:
                    Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z, x + 2, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    break;
            }
        }
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}