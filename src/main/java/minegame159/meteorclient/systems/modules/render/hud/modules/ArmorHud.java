/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.render.hud.HUD;
import minegame159.meteorclient.systems.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.render.RenderUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ArmorHud extends HudElement {
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

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> flipOrder = sgGeneral.add(new BoolSetting.Builder()
            .name("flip-order")
            .description("Flips the order of armor items.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ArmorHud.Orientation> orientation = sgGeneral.add(new EnumSetting.Builder<ArmorHud.Orientation>()
            .name("orientation")
            .description("How to display armor.")
            .defaultValue(ArmorHud.Orientation.Horizontal)
            .build()
    );

    private final Setting<ArmorHud.Durability> durability = sgGeneral.add(new EnumSetting.Builder<ArmorHud.Durability>()
            .name("durability")
            .description("How to display armor durability.")
            .defaultValue(ArmorHud.Durability.Default)
            .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of armor.")
            .defaultValue(2)
            .min(1)
            .sliderMin(1)
            .sliderMax(5)
            .build()
    );

    public ArmorHud(HUD hud) {
        super(hud, "armor", "Displays information about your armor.");
    }

    @Override
    public void update(HudRenderer renderer) {
        switch (orientation.get()) {
            case Horizontal:
                box.setSize(16 * scale.get() * 4 + 2 * 4, 16 * scale.get());
                break;
            case Vertical:
                box.setSize(16 * scale.get(), 16 * scale.get() * 4 + 2 * 4);
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        double armorX;
        double armorY;

        int slot = flipOrder.get() ? 3 : 0;
        for (int position = 0; position < 4; position++) {
            ItemStack itemStack = getItem(slot);

            RenderSystem.pushMatrix();
            RenderSystem.scaled(scale.get(), scale.get(), 1);

            if (orientation.get() == Orientation.Vertical) {
                armorX = x / scale.get();
                armorY = y / scale.get() + position * 18;
            } else {
                armorX = x / scale.get() + position * 18;
                armorY = y / scale.get();
            }

            RenderUtils.drawItem(itemStack, (int) armorX, (int) armorY, (itemStack.isDamageable() && durability.get() == Durability.Default));

            if (itemStack.isDamageable() && !isInEditor() && durability.get() != Durability.Default && durability.get() != Durability.None) {
                String message = "err";

                switch (durability.get()) {
                    case Numbers:
                        message = Integer.toString(itemStack.getMaxDamage() - itemStack.getDamage());
                        break;
                    case Percentage:
                        message = Integer.toString(Math.round(((itemStack.getMaxDamage() - itemStack.getDamage()) * 100f) / (float) itemStack.getMaxDamage()));
                        break;
                }

                double messageWidth = renderer.textWidth(message);

                if (orientation.get() == Orientation.Vertical) {
                    armorX = x + 8 * scale.get() - messageWidth / 2.0;
                    armorY = y + (18 * position * scale.get()) + (18 * scale.get() - renderer.textHeight());
                } else {
                    armorX = x + 18 * position * scale.get() + 8 * scale.get() - messageWidth / 2.0;
                    armorY = y + (box.height - renderer.textHeight());
                }

                renderer.text(message, armorX, armorY, hud.primaryColor.get());
            }

            RenderSystem.popMatrix();

            if (flipOrder.get()) slot--;
            else slot++;
        }
    }

    private ItemStack getItem(int i) {
        if (isInEditor()) {
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
