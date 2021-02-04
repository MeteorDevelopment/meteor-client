/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudEditorScreen;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.render.RenderUtils;
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
        switch (hud.armorInfoOrientation.get()) {
            case Horizontal:
                box.setSize(16 * hud.armorInfoScale.get() * 4 + 2 * 4, 16 * hud.armorInfoScale.get());
                break;
            case Vertical:
                box.setSize(16 * hud.armorInfoScale.get(), 16 * hud.armorInfoScale.get() * 4 + 2 * 4);
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        double armorX;
        double armorY;

        int slot = hud.armorInfoFlip.get() ? 3 : 0;
        for (int position = 0; position < 4; position++) {
            ItemStack itemStack = getItem(slot);

            if (hud.armorInfoOrientation.get() == Orientation.Vertical) {
                armorX = x / hud.armorInfoScale.get();
                armorY = y / hud.armorInfoScale.get() + position * 18;
            } else {
                armorX = x / hud.armorInfoScale.get() + position * 18;
                armorY = y / hud.armorInfoScale.get();
            }

            RenderUtils.drawItem(itemStack, (int) armorX, (int) armorY, hud.armorInfoScale.get(), (itemStack.isDamageable() && hud.armorInfoDurability.get() == Durability.Default));

            mc.getItemRenderer().renderGuiItemIcon(itemStack, (int) armorX, (int) armorY);

            if (itemStack.isDamageable() && !(mc.currentScreen instanceof HudEditorScreen) && hud.armorInfoDurability.get() != Durability.Default && hud.armorInfoDurability.get() != Durability.None) {
                String message = "err";

                switch (hud.armorInfoDurability.get()) {
                    case Numbers:
                        message = Integer.toString(itemStack.getMaxDamage() - itemStack.getDamage());
                        break;
                    case Percentage:
                        message = Integer.toString(Math.round(((itemStack.getMaxDamage() - itemStack.getDamage()) * 100f) / (float) itemStack.getMaxDamage()));
                        break;
                }

                double messageWidth = renderer.textWidth(message);

                if (hud.armorInfoOrientation.get() == Orientation.Vertical) {
                    armorX = x + 8 * hud.armorInfoScale.get() - messageWidth / 2.0;
                    armorY = y + (18 * position * hud.armorInfoScale.get()) + (18 * hud.armorInfoScale.get() - renderer.textHeight());
                } else {
                    armorX = x + 18 * position * hud.armorInfoScale.get() + 8 * hud.armorInfoScale.get() - messageWidth / 2.0;
                    armorY = y + (box.height - renderer.textHeight());
                }

                renderer.text(message, armorX, armorY, hud.primaryColor.get());
            }

            if (hud.armorInfoFlip.get()) slot--;
            else slot++;
        }
    }

    private ItemStack getItem(int i) {
        if (mc.player == null || mc.currentScreen instanceof HudEditorScreen) {
            switch (i) {
                default: return Items.NETHERITE_BOOTS.getDefaultStack();
                case 1:  return Items.NETHERITE_LEGGINGS.getDefaultStack();
                case 2:  return Items.NETHERITE_CHESTPLATE.getDefaultStack();
                case 3:  return Items.NETHERITE_HELMET.getDefaultStack();
            }
        }
        return mc.player.inventory.getArmorStack(i);
    }
}
