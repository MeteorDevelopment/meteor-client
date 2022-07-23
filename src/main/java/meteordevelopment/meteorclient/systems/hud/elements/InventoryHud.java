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
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InventoryHud extends HudElement {
    public static final HudElementInfo<InventoryHud> INFO = new HudElementInfo<>(Hud.GROUP, "inventory", "Displays your inventory.", InventoryHud::new);

    private static final Identifier TEXTURE = new MeteorIdentifier("textures/container.png");
    private static final Identifier TEXTURE_TRANSPARENT = new MeteorIdentifier("textures/container-transparent.png");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> containers = sgGeneral.add(new BoolSetting.Builder()
        .name("containers")
        .description("Shows the contents of a container when holding them.")
        .defaultValue(false)
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

    private final Setting<Background> background = sgGeneral.add(new EnumSetting.Builder<Background>()
        .name("background")
        .description("Background of inventory viewer.")
        .defaultValue(Background.Texture)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the background.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> background.get() != Background.None)
        .build()
    );

    private final ItemStack[] containerItems = new ItemStack[9 * 3];

    private InventoryHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(background.get().width * scale.get(), background.get().height * scale.get());

        double x = this.x, y = this.y;

        ItemStack container = getContainer();
        boolean hasContainer = containers.get() && container != null;
        if (hasContainer) Utils.getItemsInContainerItem(container, containerItems);
        Color drawColor = hasContainer ? Utils.getShulkerColor(container) : color.get();

        if (background.get() != Background.None) {
            drawBackground(renderer, (int) x, (int) y, drawColor);
        }

        if (mc.player != null) {
            for (int row = 0; row < 3; row++) {
                for (int i = 0; i < 9; i++) {
                    int index = row * 9 + i;
                    ItemStack stack = hasContainer ? containerItems[index] : mc.player.getInventory().getStack(index + 9);
                    if (stack == null) continue;

                    int itemX = background.get() == Background.Texture ? (int) (x + (8 + i * 18) * scale.get()) : (int) (x + (1 + i * 18) * scale.get());
                    int itemY = background.get() == Background.Texture ? (int) (y + (7 + row * 18) * scale.get()) : (int) (y + (1 + row * 18) * scale.get());

                    RenderUtils.drawItem(stack, itemX, itemY, scale.get(), true);
                }
            }
        }
    }

    private void drawBackground(HudRenderer renderer, int x, int y, Color color) {
        int w = getWidth();
        int h = getHeight();

        switch (background.get()) {
            case Texture, Outline -> renderer.texture(background.get() == Background.Texture ? TEXTURE : TEXTURE_TRANSPARENT, x, y, w, h, color);
            case Flat -> renderer.quad(x, y, w, h, color);
        }
    }

    private ItemStack getContainer() {
        if (isInEditor()) return null;

        ItemStack stack = mc.player.getOffHandStack();
        if (Utils.hasItems(stack) || stack.getItem() == Items.ENDER_CHEST) return stack;

        stack = mc.player.getMainHandStack();
        if (Utils.hasItems(stack) || stack.getItem() == Items.ENDER_CHEST) return stack;

        return null;
    }

    public enum Background {
        None(162, 54),
        Texture(176, 67),
        Outline(162, 54),
        Flat(162, 54);

        private final int width, height;

        Background(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
