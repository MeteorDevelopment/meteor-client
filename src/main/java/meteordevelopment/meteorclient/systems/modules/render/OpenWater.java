/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixininterface.IFishingBobberEntity;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.BlockPos;

public class OpenWater extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> openColor = sgGeneral.add(new ColorSetting.Builder()
        .name("open-color")
        .description("The color of open water.")
        .defaultValue(new SettingColor(0, 255, 0, 75))
        .build()
    );

    private final Setting<SettingColor> shallowColor = sgGeneral.add(new ColorSetting.Builder()
        .name("shallow-color")
        .description("The color of shallow water.")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public OpenWater() {
        super(Categories.Render, "open-water", "Render a open water box.");
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        FishingBobberEntity bobber = mc.player.fishHook;
        if (bobber == null) return;
        BlockPos pos = bobber.getBlockPos();
        Color color = ((IFishingBobberEntity) bobber).inOpenWater(pos) ? openColor.get() : shallowColor.get();
        event.renderer.box(pos.getX() - 2, pos.getY() - 1, pos.getZ() - 2, pos.getX() + 3, pos.getY() + 2, pos.getZ() + 3, color, color, shapeMode.get(), 0);
    }
}
