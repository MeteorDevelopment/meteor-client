/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class AirPlace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRange = settings.createGroup("range");

    // General

    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
        .name("render")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    // Range

    private final Setting<Boolean> customRange = sgRange.add(new BoolSetting.Builder()
        .name("custom-range")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> range = sgRange.add(new DoubleSetting.Builder()
        .name("range")
        .visible(customRange::get)
        .defaultValue(5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private HitResult hitResult;

    public AirPlace() {
        super(Categories.Player, "air-place");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!InvUtils.testInHands(this::placeable)) return;
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() != HitResult.Type.MISS) return;

        double r = customRange.get() ? range.get() : mc.player.getBlockInteractionRange();
        hitResult = mc.getCameraEntity().raycast(r, 0, false);
    }

    @EventHandler
    private void onInteractItem(InteractItemEvent event) {
        if (!(hitResult instanceof BlockHitResult bhr) || !placeable(mc.player.getStackInHand(event.hand))) return;

        Block toPlace = Blocks.OBSIDIAN;
        Item i = mc.player.getStackInHand(event.hand).getItem();
        if (i instanceof BlockItem blockItem) toPlace = blockItem.getBlock();
        if (!BlockUtils.canPlaceBlock(bhr.getBlockPos(), (i instanceof ArmorStandItem || i instanceof BlockItem), toPlace)) return;

        Vec3d hitPos = Vec3d.ofCenter(bhr.getBlockPos());

        BlockHitResult b = new BlockHitResult(hitPos, mc.player.getMovementDirection().getOpposite(), bhr.getBlockPos(), false);
        BlockUtils.interact(b, event.hand, true);

        event.toReturn = ActionResult.SUCCESS;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!(hitResult instanceof BlockHitResult bhr)
            || (mc.crosshairTarget != null && mc.crosshairTarget.getType() != HitResult.Type.MISS)
            || !mc.world.getBlockState(bhr.getBlockPos()).isReplaceable()
            || !InvUtils.testInHands(this::placeable)
            || !render.get()) return;

        event.renderer.box(bhr.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private boolean placeable(ItemStack stack) {
        Item i = stack.getItem();
        return i instanceof BlockItem || i instanceof SpawnEggItem || i instanceof FireworkRocketItem || i instanceof ArmorStandItem;
    }
}
