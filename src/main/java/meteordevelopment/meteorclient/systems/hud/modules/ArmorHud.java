/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ArmorHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> flipOrder = sgGeneral.add(new BoolSetting.Builder()
        .name("flip-order")
        .description("Flips the order of armor items.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Orientation> orientation = sgGeneral.add(new EnumSetting.Builder<Orientation>()
        .name("orientation")
        .description("How to display armor.")
        .defaultValue(Orientation.Horizontal)
        .build()
    );

    private final Setting<Durability> durability = sgGeneral.add(new EnumSetting.Builder<Durability>()
        .name("durability")
        .description("How to display armor durability.")
        .defaultValue(Durability.Bar)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    public ArmorHud(HUD hud) {
        super(hud, "armor", "Displays information about your armor.");
    }

    @Override
    public void update(HudRenderer renderer) {
        switch (orientation.get()) {
            case Horizontal -> box.setSize(16 * scale.get() * 4 + 2 * 4, 16 * scale.get());
            case Vertical -> box.setSize(16 * scale.get(), 16 * scale.get() * 4 + 2 * 4);
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        double armorX;
        double armorY;

        MatrixStack matrices = RenderSystem.getModelViewStack();

        matrices.push();
        matrices.scale(scale.get().floatValue(), scale.get().floatValue(), 1);

        int slot = flipOrder.get() ? 3 : 0;
        for (int position = 0; position < 4; position++) {
            ItemStack itemStack = getItem(slot);

            if (orientation.get() == Orientation.Vertical) {
                armorX = x / scale.get();
                armorY = y / scale.get() + position * 18;
            } else {
                armorX = x / scale.get() + position * 18;
                armorY = y / scale.get();
            }

            RenderUtils.drawItem(itemStack, (int) armorX, (int) armorY, (itemStack.isDamageable() && durability.get() == Durability.Bar));

            if (itemStack.isDamageable() && !isInEditor() && durability.get() != Durability.Bar && durability.get() != Durability.None) {
                String message = switch (durability.get()) {
                    case Total -> Integer.toString(itemStack.getMaxDamage() - itemStack.getDamage());
                    case Percentage -> Integer.toString(Math.round(((itemStack.getMaxDamage() - itemStack.getDamage()) * 100f) / (float) itemStack.getMaxDamage()));
                    default -> "err";
                };

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

            if (flipOrder.get()) slot--;
            else slot++;
        }

        matrices.pop();
    }

    private ItemStack getItem(int i) {
        if (isInEditor()) {
            return switch (i) {
                default -> Items.NETHERITE_BOOTS.getDefaultStack();
                case 1 -> Items.NETHERITE_LEGGINGS.getDefaultStack();
                case 2 -> Items.NETHERITE_CHESTPLATE.getDefaultStack();
                case 3 -> Items.NETHERITE_HELMET.getDefaultStack();
            };
        }

        return mc.player.getInventory().getArmorStack(i);
    }

    public enum Durability {
        None,
        Bar,
        Total,
        Percentage
    }

    public enum Orientation {
        Horizontal,
        Vertical
    }
}
