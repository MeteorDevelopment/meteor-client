/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.Render3DEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.renderer.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.entity.TargetUtils;
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
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDelay = settings.createGroup("Delay");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The maximum range for players to be targeted.")
        .defaultValue(4)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("The minimum damage to inflict on your target.")
        .defaultValue(7)
        .min(0).max(36)
        .sliderMax(36)
        .build()
    );

    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-self-damage")
        .description("The maximum damage to inflict on yourself.")
        .defaultValue(7)
        .min(0).max(36)
        .sliderMax(36)
        .build()
    );

    private final Setting<Boolean> antiSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-suicide")
        .description("Will not place and break crystals if they will kill you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoMove = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-move")
        .description("Moves beds into a selected hotbar slot.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> autoMoveSlot = sgGeneral.add(new IntSetting.Builder()
        .name("auto-move-slot")
        .description("The slot auto move moves beds to.")
        .defaultValue(9)
        .min(1).max(9)
        .sliderMin(1).sliderMax(9)
        .visible(autoMove::get)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches to and from beds automatically.")
        .defaultValue(true)
        .build()
    );

    // Delay

    private final Setting<Integer> placeDelay = sgDelay.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The tick delay for placing beds.")
        .defaultValue(9)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Integer> breakDelay = sgDelay.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The tick delay for breaking beds.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );

    // Pause

    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses while eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-drink")
        .description("Pauses while drinking.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses while mining.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Whether to swing hand clientside clientside.")
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
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color for positions to be placed.")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color for positions to be placed.")
        .defaultValue(new SettingColor(15, 255, 211, 255))
        .build()
    );

    private Direction direction;
    private PlayerEntity target;
    private BlockPos bestPos;
    private int breakDelayLeft;
    private int placeDelayLeft;
    private Stage stage;

    public BedAura() {
        super(Categories.Combat, "bed-aura", "Automatically places and explodes beds in the Nether and End.");
    }

    @Override
    public void onActivate() {
        stage = Stage.Placing;

        bestPos = null;

        direction = Direction.EAST;

        placeDelayLeft = placeDelay.get();
        breakDelayLeft = placeDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world.getDimension().isBedWorking()) {
            error("You are in the Overworld... disabling!");
            toggle();
            return;
        }

        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;

        target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (target == null) {
            bestPos = null;
            return;
        }

        switch (stage) {
            case Placing:
                if (!InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) {
                    placeDelayLeft = placeDelay.get();
                    stage = Stage.Breaking;
                    break;
                }

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
    }

    // Place

    private BlockPos getPlacePos(PlayerEntity target) {
        for (int index = 0; index < 3; index++) {
            int i = index == 0 ? 1 : index == 1 ? 0 : 2; // troll

            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;

                BlockPos centerPos = target.getBlockPos().up(i);

                double headSelfDamage = DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(centerPos));
                double offsetSelfDamage = DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(centerPos.offset(dir)));

                if (mc.world.getBlockState(centerPos).getMaterial().isReplaceable()
                    && BlockUtils.canPlace(centerPos.offset(dir))
                    && DamageCalcUtils.bedDamage(target, Utils.vec3d(centerPos)) >= minDamage.get()
                    && offsetSelfDamage < maxSelfDamage.get()
                    && headSelfDamage < maxSelfDamage.get()
                    && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - headSelfDamage > 0)
                    && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0)) {
                    direction = dir;
                    return centerPos.offset(direction);
                }
            }
        }

        return null;
    }

    private void placeBed(BlockPos pos) {
        FindItemResult result = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);

        if (result.isMain() && autoMove.get()) doAutoMove();

        result = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!result.found()) return;
        if (result.getHand() == null && !autoSwitch.get()) return;

        FindItemResult finalRes = result;

        double yaw = switch (direction) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> -90;
            default -> 0;
        };

        Rotations.rotate(yaw, mc.player.getPitch(), () -> BlockUtils.place(pos, finalRes, false, 0, swing.get(), true, autoSwitch.get()));
    }

    private void doAutoMove() {
        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);

        if (bed.found() && bed.getSlot() != autoMoveSlot.get() - 1) {
            InvUtils.move().from(bed.getSlot()).toHotbar(autoMoveSlot.get() - 1);
        }
    }

    // Break

    private BlockPos getBreakPos(PlayerEntity target) {
        for (int index = 0; index < 3; index++) {
            int i = index == 0 ? 1 : index == 1 ? 0 : 2; // troll

            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;

                BlockPos centerPos = target.getBlockPos().up(i);

                double headSelfDamage = DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(centerPos));
                double offsetSelfDamage = DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(centerPos.offset(dir)));

                if (mc.world.getBlockState(centerPos).getBlock() instanceof BedBlock
                    && mc.world.getBlockState(centerPos.offset(dir)).getBlock() instanceof BedBlock
                    && DamageCalcUtils.bedDamage(target, Utils.vec3d(centerPos)) >= minDamage.get()
                    && offsetSelfDamage < maxSelfDamage.get()
                    && headSelfDamage < maxSelfDamage.get()
                    && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - headSelfDamage > 0)
                    && (!antiSuicide.get() || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0)) {
                    direction = dir;
                    return centerPos.offset(direction);
                }
            }
        }

        return null;
    }

    private void breakBed(BlockPos pos) {
        if (pos == null) return;

        boolean wasSneaking = mc.player.isSneaking();
        if (wasSneaking) mc.player.setSneaking(false);

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.OFF_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, bestPos, false));

        mc.player.setSneaking(wasSneaking);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && bestPos != null) {
            int x = bestPos.getX();
            int y = bestPos.getY();
            int z = bestPos.getZ();

            switch (direction) {
                case NORTH -> event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case SOUTH -> event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case EAST -> event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case WEST -> event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    private enum Stage {
        Placing,
        Breaking
    }
}
