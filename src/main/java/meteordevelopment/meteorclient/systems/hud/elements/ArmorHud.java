/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ArmorHud extends HudElement {
    public static final HudElementInfo<ArmorHud> INFO = new HudElementInfo<>(Hud.GROUP, "armor", "Displays your armor.", ArmorHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDurability = settings.createGroup("Durability");
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    private final Setting<Orientation> orientation = sgGeneral.add(new EnumSetting.Builder<Orientation>()
        .name("orientation")
        .description("How to display armor.")
        .defaultValue(Orientation.Horizontal)
        .onChanged(_ -> calculateSize(HudRenderer.INSTANCE))
        .build()
    );

    private final Setting<Boolean> flipOrder = sgGeneral.add(new BoolSetting.Builder()
        .name("flip-order")
        .description("Flips the order of armor items.")
        .defaultValue(true)
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
        .onChanged(_ -> calculateSize(HudRenderer.INSTANCE))
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

    // Scale

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .onChanged(_ -> calculateSize(HudRenderer.INSTANCE))
        .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(2)
        .onChanged(_ -> calculateSize(HudRenderer.INSTANCE))
        .min(0.5)
        .sliderRange(0.5, 3)
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

        calculateSize(HudRenderer.INSTANCE);
    }

    @Override
    public void tick(HudRenderer renderer) {
        calculateSize(renderer);
    }

    private boolean showsDurabilityText() {
        return durability.get() == Durability.Total || durability.get() == Durability.Percentage;
    }

    private double getIconSize() {
        return 16 * getScale();
    }

    private double getPadding() {
        return 2 * getScale();
    }

    // Durability text scale relative to the icon scale: 1x (normal HUD text scale) at the default
    // icon scale, growing/shrinking as the icon's custom scale changes. Uses a square root so the
    // text doesn't grow as aggressively as the icon does at the higher end of the scale slider.
    private double getTextScale() {
        return Math.sqrt(getScale() / scale.getDefaultValue().floatValue()) * Hud.get().getTextScale();
    }

    // Spacing between items along the main layout axis: columns in Horizontal, rows in Vertical.
    // The durability text doesn't affect this since it's drawn on the cross axis (below in Horizontal,
    // to the right in Vertical), not between items.
    private double getItemStride() {
        return getIconSize() + getPadding();
    }

    private ItemStack[] getArmorItems() {
        // default order is from boots to helmet
        return flipOrder.get() ?
            new ItemStack[]{getItem(EquipmentSlot.HEAD), getItem(EquipmentSlot.CHEST), getItem(EquipmentSlot.LEGS), getItem(EquipmentSlot.FEET)} :
            new ItemStack[]{getItem(EquipmentSlot.FEET), getItem(EquipmentSlot.LEGS), getItem(EquipmentSlot.CHEST), getItem(EquipmentSlot.HEAD)};
    }

    private String getDurabilityText(ItemStack itemStack) {
        return switch (durability.get()) {
            case Total -> Integer.toString(itemStack.getMaxDamage() - itemStack.getDamageValue());
            case Percentage ->
                Integer.toString(Math.round(((itemStack.getMaxDamage() - itemStack.getDamageValue()) * 100f) / (float) itemStack.getMaxDamage()));
            default -> "";
        };
    }

    // Widest durability text among the currently displayed items, used to size the Vertical orientation's width.
    private double getMaxDurabilityTextWidth(HudRenderer renderer) {
        double maxWidth = 0;

        for (ItemStack stack : getArmorItems()) {
            if (!stack.isDamageableItem()) continue;
            maxWidth = Math.max(maxWidth, renderer.textWidth(getDurabilityText(stack), durabilityShadow.get(), getTextScale()));
        }

        return maxWidth;
    }

    private void calculateSize(HudRenderer renderer) {
        double iconSize = getIconSize();
        double stride = getItemStride();
        boolean showsText = showsDurabilityText();

        switch (orientation.get()) {
            case Horizontal -> {
                // Text sits below each icon, so it grows the box's height.
                double height = iconSize;
                if (showsText) height += getPadding() + renderer.textHeight(durabilityShadow.get(), getTextScale());
                setSize(stride * 4, height);
            }
            case Vertical -> {
                // Text sits to the right of each icon, so it grows the box's width instead of its height.
                double width = iconSize;
                if (showsText) width += getPadding() + getMaxDurabilityTextWidth(renderer);
                setSize(width, stride * 4);
            }
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        ItemStack[] armor = getArmorItems();

        int emptySlots = 0;
        for (ItemStack stack : armor) {
            if (stack.isEmpty()) emptySlots++;
        }

        if (background.get() && emptySlots < 4) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }

        renderer.post(() -> {
            double x = this.x;
            double y = this.y;

            double iconSize = getIconSize();
            double padding = getPadding();
            double stride = getItemStride();
            boolean showsText = showsDurabilityText();
            boolean vertical = orientation.get() == Orientation.Vertical;

            double armorX, armorY;

            for (int position = 0; position < 4; position++) {
                ItemStack itemStack = armor[position];

                if (vertical) {
                    armorX = x;
                    armorY = y + position * stride;
                } else {
                    armorX = x + position * stride;
                    armorY = y;
                }

                renderer.item(itemStack, (int) armorX, (int) armorY, getScale(), (itemStack.isDamageableItem() && durability.get() == Durability.Bar));

                if (itemStack.isDamageableItem() && showsText && durability.get() != Durability.Bar) {
                    String message = getDurabilityText(itemStack);
                    double messageWidth = renderer.textWidth(message, durabilityShadow.get(), getTextScale());
                    double textHeight = renderer.textHeight(durabilityShadow.get(), getTextScale());

                    double textX, textY;
                    if (vertical) {
                        textX = armorX + iconSize + padding;
                        textY = armorY + iconSize / 2.0 - textHeight / 2.0;
                    } else {
                        textX = armorX + iconSize / 2.0 - messageWidth / 2.0;
                        textY = armorY + iconSize + padding;
                    }

                    renderer.text(message, textX, textY, durabilityColor.get(), durabilityShadow.get(), getTextScale());
                }
            }
        });
    }

    private ItemStack getItem(EquipmentSlot slot) {
        if (isInEditor()) {
            return switch (slot.getIndex()) {
                case 3 -> DisplayItemUtils.toStack(Items.NETHERITE_HELMET);
                case 2 -> DisplayItemUtils.toStack(Items.NETHERITE_CHESTPLATE);
                case 1 -> DisplayItemUtils.toStack(Items.NETHERITE_LEGGINGS);
                default -> DisplayItemUtils.toStack(Items.NETHERITE_BOOTS);
            };
        }

        ItemStack stack = mc.player.getItemBySlot(slot);
        return stack.isEmpty() && showEmpty.get() ? DisplayItemUtils.toStack(Items.BARRIER) : stack;
    }

    private float getScale() {
        return customScale.get() ? scale.get().floatValue() : scale.getDefaultValue().floatValue();
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
