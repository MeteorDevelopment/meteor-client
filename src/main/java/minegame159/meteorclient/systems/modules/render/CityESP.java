/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class CityESP extends Module {
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the rendering.")
            .defaultValue(new SettingColor(225, 0, 0, 75))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the rendering.")
            .defaultValue(new SettingColor(225, 0, 0, 255))
            .build()
    );

    private BlockPos target;

    public CityESP() {
        super(Categories.Render, "city-esp", "Displays blocks that can be broken in order to city another player.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        PlayerEntity targetEntity = EntityUtils.getPlayerTarget(mc.interactionManager.getReachDistance() + 2, SortPriority.LowestDistance, false);

        if (EntityUtils.isBadTarget(targetEntity, mc.interactionManager.getReachDistance() + 2)) {
            target = null;
        } else {
            target = EntityUtils.getCityBlock(targetEntity);
        }
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (target == null) return;
        Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, target, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
