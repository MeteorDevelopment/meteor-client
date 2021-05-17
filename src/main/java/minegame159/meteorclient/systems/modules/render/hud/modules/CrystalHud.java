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

//SonyTV was here :)

public class CrystalHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of crystal counter.")
            .defaultValue(3)
            .min(1)
            .sliderMin(1)
            .sliderMax(4)
            .build()
    );

    public CrystalHud(HUD hud) {
        super(hud, "crytals", "Displays the amount of crystals in your inventory.", false);
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
            RenderUtils.drawItem(Items.END_CRYSTAL.getDefaultStack(), (int) x, (int) y, scale.get(), true);
        } else if (InvUtils.findItemWithCount(Items.END_CRYSTAL).count > 0) {
            RenderUtils.drawItem(new ItemStack(Items.END_CRYSTAL, InvUtils.findItemWithCount(Items.END_CRYSTAL).count), (int) x, (int) y, scale.get(), true);
        }
    }
}