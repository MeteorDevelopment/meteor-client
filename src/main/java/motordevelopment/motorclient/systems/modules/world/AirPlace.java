/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.world;

import net.minecraft.item.SpawnEggItem;
import motordevelopment.motorclient.events.render.Render3DEvent;
import motordevelopment.motorclient.events.world.TickEvent;
import motordevelopment.motorclient.renderer.ShapeMode;
import motordevelopment.motorclient.settings.*;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.utils.render.color.SettingColor;
import motordevelopment.motorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AirPlace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRange = settings.createGroup("Range");

    // General

    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the obsidian will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    // Range

    private final Setting<Boolean> customRange = sgRange.add(new BoolSetting.Builder()
        .name("custom-range")
        .description("Use custom range for air place.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> range = sgRange.add(new DoubleSetting.Builder()
        .name("range")
        .description("Custom range to place at.")
        .visible(customRange::get)
        .defaultValue(5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private HitResult hitResult;

    public AirPlace() {
        super(Categories.Player, "air-place", "Places a block where your crosshair is pointing at.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        double r = customRange.get() ? range.get() : mc.player.getBlockInteractionRange();
        hitResult = mc.getCameraEntity().raycast(r, 0, false);

        if (!(hitResult instanceof BlockHitResult blockHitResult) || !(mc.player.getMainHandStack().getItem() instanceof BlockItem) && !(mc.player.getMainHandStack().getItem() instanceof SpawnEggItem)) return;

        if (mc.options.useKey.isPressed()) {
            BlockUtils.place(blockHitResult.getBlockPos(), Hand.MAIN_HAND, mc.player.getInventory().selectedSlot, false, 0, true, true, false);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!(hitResult instanceof BlockHitResult blockHitResult)
            || !mc.world.getBlockState(blockHitResult.getBlockPos()).isReplaceable()
            || !(mc.player.getMainHandStack().getItem() instanceof BlockItem) && !(mc.player.getMainHandStack().getItem() instanceof SpawnEggItem)
            || !render.get()) return;

        event.renderer.box(blockHitResult.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
