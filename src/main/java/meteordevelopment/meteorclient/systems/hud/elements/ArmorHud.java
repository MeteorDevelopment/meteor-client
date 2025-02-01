/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ArmorHud extends HudElement {
    public static final HudElementInfo<ArmorHud> INFO = new HudElementInfo<>(Hud.GROUP, "armor", "Displays your armor.", ArmorHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDurability = settings.createGroup("Durability");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    private final Setting<Orientation> orientation = sgGeneral.add(new EnumSetting.Builder<Orientation>()
        .name("orientation")
        .description("How to display armor.")
        .defaultValue(Orientation.Horizontal)
        .onChanged(val -> calculateSize())
        .build()
    );

    private final Setting<Boolean> flipOrder = sgGeneral.add(new BoolSetting.Builder()
        .name("flip-order")
        .description("Flips the order of armor items.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(2)
        .onChanged(aDouble -> calculateSize())
        .min(0.5)
        .sliderRange(0.5, 5)
        .build()
    );

    private final Setting<Boolean> showEmpty = sgGeneral.add(new BoolSetting.Builder()
        .name("show-empty")
        .description("Renders barrier icons for empty slots.")
        .defaultValue(false)
        .build()
    );

    // Durability

    private final Setting<Durability> durability = sgDurability.add(new EnumSetting.Builder<Durability>()
        .name("durability")
        .description("How to display armor durability.")
        .defaultValue(Durability.Bar)
        .onChanged(durability1 -> calculateSize())
        .build()
    );

    private final Setting<SettingColor> durabilityColor = sgDurability.add(new ColorSetting.Builder()
        .name("durability-color")
        .description("Color of the text.")
        .visible(() -> durability.get() == Durability.Total || durability.get() == Durability.Percentage)
        .defaultValue(new SettingColor())
        .build()
    );

    private final Setting<Boolean> durabilityShadow = sgDurability.add(new BoolSetting.Builder()
        .name("durability-shadow")
        .description("Text shadow.")
        .visible(() -> durability.get() == Durability.Total || durability.get() == Durability.Percentage)
        .defaultValue(true)
        .build()
    );

    // Background

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    public ArmorHud() {
        super(INFO);

        calculateSize();
    }

    private void calculateSize() {
        switch (orientation.get()) {
            case Horizontal -> setSize(16 * scale.get() * 4 + 2 * 4 * scale.get(), 16 * scale.get());
            case Vertical -> setSize(16 * scale.get(), 16 * scale.get() * 4 + 2 * 4 * scale.get());
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        int emptySlots = 0;

        // default order is from boots to helmet
        ItemStack[] armor = flipOrder.get() ?
            new ItemStack[]{getItem(3), getItem(2), getItem(1), getItem(0)} :
            new ItemStack[]{getItem(0), getItem(1), getItem(2), getItem(3)};

        for (ItemStack stack : armor) {
            if (stack.isEmpty()) emptySlots++;
        }

        if (background.get() && emptySlots < 4) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }

        renderer.post(() -> {
            double x = this.x;
            double y = this.y;

            double armorX, armorY;

            for (int position = 0; position < 4; position++) {
                ItemStack itemStack = armor[position];

                if (orientation.get() == Orientation.Vertical) {
                    armorX = x;
                    armorY = y + position * 18 * scale.get();
                } else {
                    armorX = x + position * 18 * scale.get();
                    armorY = y;
                }

                renderer.item(itemStack, (int) armorX, (int) armorY, scale.get().floatValue(), (itemStack.isDamageable() && durability.get() == Durability.Bar));

                if (itemStack.isDamageable() && durability.get() != Durability.Bar && durability.get() != Durability.None) {
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
                        armorY = y + (getHeight() - renderer.textHeight());
                    }

                    TextRenderer.get().render(message, armorX, armorY, durabilityColor.get(), durabilityShadow.get());
                }
            }
        });
    }

    private ItemStack getItem(int i) {
        if (isInEditor()) {
            return switch (i) {
                case 3 -> Items.NETHERITE_HELMET.getDefaultStack();
                case 2 -> Items.NETHERITE_CHESTPLATE.getDefaultStack();
                case 1 -> Items.NETHERITE_LEGGINGS.getDefaultStack();
                default -> Items.NETHERITE_BOOTS.getDefaultStack();
            };
        }

        ItemStack stack = mc.player.getInventory().getArmorStack(i);
        return stack.isEmpty() && showEmpty.get() ? Items.BARRIER.getDefaultStack() : stack;
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
