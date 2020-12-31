/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.player.CityUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class CityESP extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("The maximum range a city-able block will render.")
            .defaultValue(5)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> checkBelow = sgGeneral.add(new BoolSetting.Builder()
            .name("check-below")
            .description("Checks if there is an obsidian/bedrock block below the surround block for you to place crystals on.")
            .defaultValue(true)
            .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("fill-color")
            .description("The fill color the city block will render as.")
            .defaultValue(new SettingColor(225, 0, 0, 75))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("outline-color")
            .description("The line color the city block will render as.")
            .defaultValue(new SettingColor(225, 0, 0, 255))
            .build()
    );

    public CityESP() {
        super(Category.Render, "city-esp", "Displays blocks that can be broken in order to city another player.");
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        PlayerEntity target = CityUtils.getPlayerTarget();
        BlockPos targetBlock = CityUtils.getTargetBlock(checkBelow.get());

        if (target == null || targetBlock == null || MathHelper.sqrt(mc.player.squaredDistanceTo(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ())) > range.get()) return;

        int x = targetBlock.getX();
        int y = targetBlock.getY();
        int z = targetBlock.getZ();

        Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z, 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    });
}
