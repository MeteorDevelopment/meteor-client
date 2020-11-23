/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ArmorHud extends HudModule {
    public enum Durability {
        None,
        Default,
        Numbers,
        Percentage
    }

    public ArmorHud(HUD hud) {
        super(hud, "armor", "Displays your armor.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(16 * hud.armorScale() * 4 + 2 * 4, 16 * hud.armorScale());
    }

    @Override
    public void render(HudRenderer renderer) {
        MinecraftClient mc = MinecraftClient.getInstance();

        double x = box.getX();
        double y = box.getY();

        int i = hud.armorFlip() ? 3 : 0;
        while (true) {
            if (hud.armorFlip()) {
                if (i < 0) break;
            } else {
                if (i > 3) break;
            }

            ItemStack itemStack = getItem(i);

            RenderSystem.pushMatrix();
            RenderSystem.scaled(hud.armorScale(), hud.armorScale(), 1);
            mc.getItemRenderer().renderGuiItemIcon(itemStack, (int) (x / hud.armorScale() + i * 18), (int) (y / hud.armorScale()));

            if (itemStack.isDamageable()) {
                switch (hud.armorDurability()) {
                    case Default: {
                        mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, (int) (x / hud.armorScale() + i * 18), (int) (y / hud.armorScale()));
                        break;
                    }
                    case Numbers: {
                        String message = Integer.toString(itemStack.getMaxDamage() - itemStack.getDamage());
                        double messageWidth = renderer.textWidth(message);
                        renderer.text(message, x + 18 * i * hud.armorScale() + 8 * hud.armorScale() - messageWidth / 2.0, y + (box.height - renderer.textHeight()), hud.primaryColor());
                        break;
                    }
                    case Percentage: {
                        String message = Integer.toString(Math.round(((itemStack.getMaxDamage() - itemStack.getDamage()) * 100f) / (float) itemStack.getMaxDamage()));
                        double messageWidth = renderer.textWidth(message);
                        renderer.text(message, x + 18 * i * hud.armorScale() + 8 * hud.armorScale() - messageWidth / 2.0, y + (box.height - renderer.textHeight()), hud.primaryColor());
                        break;
                    }
                }
            }

            RenderSystem.popMatrix();

            if (hud.armorFlip()) i--;
            else i++;
        }
    }

    private ItemStack getItem(int i) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            switch (i) {
                default: return Items.DIAMOND_BOOTS.getDefaultStack();
                case 1:  return Items.DIAMOND_LEGGINGS.getDefaultStack();
                case 2:  return Items.DIAMOND_CHESTPLATE.getDefaultStack();
                case 3:  return Items.DIAMOND_HELMET.getDefaultStack();
            }
        }
        return mc.player.inventory.getArmorStack(i);
    }
}
