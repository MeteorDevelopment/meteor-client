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
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.CityUtils;
import minegame159.meteorclient.utils.Color;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class CityEsp extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("The maximum range a city-able block will render.")
            .defaultValue(5)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Color> fillColor = sgGeneral.add(new ColorSetting.Builder()
            .name("fill-color")
            .description("The fill color the city block will render as.")
            .defaultValue(new Color(225, 0, 0, 75))
            .build()
    );

    private final Setting<Color> outlineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("outline-color")
            .description("The outline color the city block will render as.")
            .defaultValue(new Color(225, 0, 0, 255))
            .build()
    );

    public CityEsp() {
        super(Category.Combat, "city-esp", "Displays blocks that can be broken in order to city another player.");
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        PlayerEntity target = CityUtils.getPlayerTarget();
        BlockPos targetBlock = CityUtils.getTargetBlock();

        if (target == null || targetBlock == null || MathHelper.sqrt(mc.player.squaredDistanceTo(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ())) > range.get()) return;

        int x = targetBlock.getX();
        int y = targetBlock.getY();
        int z = targetBlock.getZ();

        ShapeBuilder.blockSides(x, y, z, fillColor.get(), null);
        ShapeBuilder.blockEdges(x, y, z, outlineColor.get(), null);
    });
}
