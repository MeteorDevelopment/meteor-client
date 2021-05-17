/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud.modules;

import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.render.hud.HUD;
import minegame159.meteorclient.systems.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.render.RenderUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ExpHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of exp bottle counter.")
            .defaultValue(3)
            .min(1)
            .sliderMin(1)
            .sliderMax(4)
            .build()
    );

    public ExpHud(HUD hud) {
        super(hud, "exp", "Displays the amount of exp bottles in your inventory.", false);
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
            RenderUtils.drawItem(Items.EXPERIENCE_BOTTLE.getDefaultStack(), (int) x, (int) y, scale.get(), true);
        } else if (InvUtils.findItemWithCount(Items.EXPERIENCE_BOTTLE).count > 0) {
            RenderUtils.drawItem(new ItemStack(Items.EXPERIENCE_BOTTLE, InvUtils.findItemWithCount(Items.EXPERIENCE_BOTTLE).count), (int) x, (int) y, scale.get(), true);
        }
    }
}