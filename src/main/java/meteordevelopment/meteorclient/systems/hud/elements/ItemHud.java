/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ItemHud extends HudElement {
    public static HudElementInfo<ItemHud> INFO = new HudElementInfo<>(Hud.GROUP, "item", "Displays the item count.", ItemHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    private final Setting<Item> item = sgGeneral.add(new ItemSetting.Builder()
        .name("item")
        .description("Item to display")
        .defaultValue(Items.TOTEM_OF_UNDYING)
        .build()
    );

    private final Setting<NoneMode> noneMode = sgGeneral.add(new EnumSetting.Builder<NoneMode>()
        .name("none-mode")
        .description("How to render the item when you don't have the specified item in your inventory.")
        .defaultValue(NoneMode.HideCount)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the item.")
        .defaultValue(2)
        .onChanged(aDouble -> calculateSize())
        .min(1)
        .sliderRange(1, 4)
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the element.")
        .defaultValue(0)
        .onChanged(integer -> calculateSize())
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

    private ItemHud() {
        super(INFO);

        calculateSize();
    }

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    private void calculateSize() {
        setSize(17 * scale.get(), 17 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        ItemStack itemStack = new ItemStack(item.get(), InvUtils.find(item.get()).count());

        if (noneMode.get() == NoneMode.HideItem && itemStack.isEmpty()) {
            if (isInEditor()) {
                renderer.line(x, y, x + getWidth(), y + getHeight(), Color.GRAY);
                renderer.line(x, y + getHeight(), x + getWidth(), y, Color.GRAY);
            }
        }
        else {
            renderer.post(() -> {
                MatrixStack matrices = RenderSystem.getModelViewStack();

                matrices.push();
                matrices.scale(scale.get().floatValue(), scale.get().floatValue(), 1);

                double x = this.x + border.get();
                double y = this.y + border.get();

                render(itemStack, (int) (x / scale.get()), (int) (y / scale.get()));

                matrices.pop();
            });
        }

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }
    }

    private void render(ItemStack itemStack, int x, int y) {
        switch (noneMode.get()) {
            case HideItem -> {
                mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
                mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, Integer.toString(itemStack.getCount()));
            }
            case HideCount -> {
                if (itemStack.getCount() == 0) itemStack.setCount(Integer.MAX_VALUE);
                mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
                if (itemStack.getCount() == Integer.MAX_VALUE) itemStack.setCount(0);

                if (!itemStack.isEmpty()) {
                    mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, Integer.toString(itemStack.getCount()));
                }
            }
            case ShowCount -> {
                if (itemStack.getCount() == 0) itemStack.setCount(Integer.MAX_VALUE);
                mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
                mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, Integer.toString(itemStack.getCount() == Integer.MAX_VALUE ? 0 : itemStack.getCount()));
                if (itemStack.getCount() == Integer.MAX_VALUE) itemStack.setCount(0);
            }
        }
    }

    public enum NoneMode {
        HideItem,
        HideCount,
        ShowCount;

        @Override
        public String toString() {
            return switch (this) {
                case HideItem -> "Hide Item";
                case HideCount -> "Hide Count";
                case ShowCount -> "Show Count";
            };
        }
    }
}
