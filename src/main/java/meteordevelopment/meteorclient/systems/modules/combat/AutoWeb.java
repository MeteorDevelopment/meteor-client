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
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

public class AutoWeb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The range at which webs can be placed.")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> placeWallsRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Range in which to place webs when behind blocks.")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.LowestDistance)
        .build()
    );

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The maximum distance to target players.")
        .defaultValue(10)
        .min(0)
        .sliderMax(30)
        .build()
    );

    private final Setting<Boolean> predictMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("predict-movement")
        .description("Predict target movement to account for ping.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> ticksToPredict = sgGeneral.add(new DoubleSetting.Builder()
        .name("ticks-to-predict")
        .description("How many ticks ahead we should predict for.")
        .defaultValue(10)
        .min(1)
        .sliderMax(30)
        .visible(predictMovement::get)
        .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
        .name("doubles")
        .description("Places webs in the target's upper hitbox as well as the lower hitbox.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the webs when placing.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders an overlay where webs are placed.")
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
        .description("The side color of the placed web rendering.")
        .defaultValue(new SettingColor(239, 231, 244, 31))
        .visible(() -> render.get() && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the placed web rendering.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> render.get() && shapeMode.get().lines())
        .build()
    );

    private final List<BlockPos> placePositions = new ArrayList<>();
    private PlayerEntity target = null;

    public AutoWeb() {
        super(Categories.Combat, "auto-web", "Automatically places webs on other players.");
    }

    @Override
    public void onActivate() {
        target = null;
    }

    @Override
    public void onDeactivate() {
        placePositions.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        placePositions.clear();

        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
            if (TargetUtils.isBadTarget(target, targetRange.get())) return;
        }

        // Grab webs from hotbar
        FindItemResult webs = InvUtils.findInHotbar(Items.COBWEB);
        if (!webs.found()) return;

        Vec3d pos = target.getPos();

        // Prediction mode via target's movement delta
        if (predictMovement.get()) {
            double dx = target.getX() - target.lastX;
            double dy = target.getY() - target.lastY;
            double dz = target.getZ() - target.lastZ;
            pos = pos.add(dx * ticksToPredict.get(), dy * ticksToPredict.get(), dz * ticksToPredict.get());
        }

        BlockPos blockPos = BlockPos.ofFloored(pos);

        if (canPlaceWebAt(blockPos)) {
            BlockUtils.place(blockPos, webs, rotate.get(), 0, false);
            placePositions.add(blockPos);
        }

        if (doubles.get() && canPlaceWebAt(blockPos.up())) {
            BlockUtils.place(blockPos.up(), webs, rotate.get(), 0, false);
            placePositions.add(blockPos.up());
        }
    }

    private boolean canPlaceWebAt(BlockPos blockPos) {
        if (!mc.world.getBlockState(blockPos).isReplaceable()) return false;

        // Check raycast and range
        return !isOutOfRange(blockPos);
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

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placePositions.isEmpty()) return;

        for (BlockPos placePosition : placePositions) {
            event.renderer.box(placePosition, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
