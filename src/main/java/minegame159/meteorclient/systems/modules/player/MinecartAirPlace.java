/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.player;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

// Basically AirPlace but works with any item just because I can.

public class MinecartAirPlace extends Module {
    public enum Place {
        OnClick,
        Always
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the minecart will be placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(188, 26, 141, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(237, 19, 144, 255))
            .build()
    );

    private BlockPos target;

    public MinecartAirPlace() {
        super(Categories.Player, "MinecartAirPlace", "Places a minecart or block where your crosshair is pointing at.");
    }

    @Override
    public void onActivate() {
        target = mc.player.getBlockPos().add(4, 2, 0);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!(mc.crosshairTarget instanceof BlockHitResult)) return;

        target = ((BlockHitResult) mc.crosshairTarget).getBlockPos();

        if (!mc.world.getBlockState(target).isAir()) return;

        if (mc.options.keyUse.wasPressed() || mc.options.keyUse.isPressed()) {
            BlockUtils.place(target, Hand.MAIN_HAND, 0, false, 0, true, true, false, false);
        }
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (!(mc.crosshairTarget instanceof BlockHitResult)
                || !mc.world.getBlockState(target).isAir()
                || !render.get()) return;

        Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, target, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
