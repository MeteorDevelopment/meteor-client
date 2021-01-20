/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ArmorHud extends HudModule {
    public enum Durability {
        None,
        Default,
        Numbers,
        Percentage
    }

    public enum Orientation {
        Horizontal,
        Vertical
    }

    public ArmorHud(HUD hud) {
        super(hud, "armor", "Displays information about your armor.");
    }

    @Override
    public void update(HudRenderer renderer) {
        switch (hud.armorOrientation()) {
            case Horizontal:
                box.setSize(16 * hud.armorScale() * 4 + 2 * 4, 16 * hud.armorScale());
                break;
            case Vertical:
                box.setSize(16 * hud.armorScale(), 16 * hud.armorScale() * 4 + 2 * 4);
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        double armorX;
        double armorY;

        int slot = hud.armorFlip() ? 3 : 0;
        for (int position = 0; position < 4; position++) {
            ItemStack itemStack = getItem(slot);

            RenderSystem.pushMatrix();
            RenderSystem.scaled(hud.armorScale(), hud.armorScale(), 1);

            if (hud.armorOrientation() == Orientation.Vertical) {
                armorX = x / hud.armorScale();
                armorY = y / hud.armorScale() + position * 18;
            } else {
                armorX = x / hud.armorScale() + position * 18;
                armorY = y / hud.armorScale();
            }
            mc.getItemRenderer().renderGuiItemIcon(itemStack, (int) armorX, (int) armorY);

            if (itemStack.isDamageable()) {
                if (hud.armorDurability() == Durability.Default) {
                    mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, (int) armorX, (int) armorY);
                } else if (hud.armorDurability() != Durability.None) {
                    String message = "sex";

                    switch (hud.armorDurability()) {
                        case Numbers:
                            message = Integer.toString(itemStack.getMaxDamage() - itemStack.getDamage());
                            break;
                        case Percentage:
                            message = Integer.toString(Math.round(((itemStack.getMaxDamage() - itemStack.getDamage()) * 100f) / (float) itemStack.getMaxDamage()));
                            break;
                    }

                    double messageWidth = renderer.textWidth(message);

                    if (hud.armorOrientation() == Orientation.Vertical) {
                        armorX = x + 8 * hud.armorScale() - messageWidth / 2.0;
                        armorY = y + (18 * position * hud.armorScale()) + (18 * hud.armorScale() - renderer.textHeight());
                    } else {
                        armorX = x + 18 * position * hud.armorScale() + 8 * hud.armorScale() - messageWidth / 2.0;
                        armorY = y + (box.height - renderer.textHeight());
                    }

                    renderer.text(message, armorX, armorY, hud.primaryColor());
                }
            }

            RenderSystem.popMatrix();

            if (hud.armorFlip()) slot--;
            else slot++;
        }
    }

    private ItemStack getItem(int i) {
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
