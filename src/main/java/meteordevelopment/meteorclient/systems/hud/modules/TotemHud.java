/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TotemHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    public TotemHud(HUD hud) {
        super(hud, "totems", "Displays the amount of totems in your inventory.", false);
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(16 * scale.get(), 16 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (isInEditor()) {
            RenderUtils.drawItem(Items.TOTEM_OF_UNDYING.getDefaultStack(), (int) x, (int) y, scale.get(), true);
        }
        else if (InvUtils.find(Items.TOTEM_OF_UNDYING).count() > 0) {
            RenderUtils.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING, InvUtils.find(Items.TOTEM_OF_UNDYING).count()), (int) x, (int) y, scale.get(), true);
        }
    }
}
