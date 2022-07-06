/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InventoryHud extends HudElement {
    public static final HudElementInfo<InventoryHud> INFO = new HudElementInfo<>(Hud.GROUP, "inventory", "Displays your inventory.", InventoryHud::new);

    private static final Identifier TEXTURE = new Identifier("meteor-client", "textures/container.png");
    private static final Identifier TEXTURE_TRANSPARENT = new Identifier("meteor-client", "textures/container-transparent.png");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    private InventoryHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(background.get().width * scale.get(), background.get().height * scale.get());

        double x = this.x;
        double y = this.y;

        if (background.get() != Background.None) {
            drawBackground(renderer, (int) x, (int) y);
        }

        if (mc.player != null) {
            for (int row = 0; row < 3; row++) {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = getStack(9 + row * 9 + i);
                    if (stack == null) continue;

                    int itemX = background.get() == Background.Texture ? (int) (x + (8 + i * 18) * scale.get()) : (int) (x + (1 + i * 18) * scale.get());
                    int itemY = background.get() == Background.Texture ? (int) (y + (7 + row * 18) * scale.get()) : (int) (y + (1 + row * 18) * scale.get());

                    RenderUtils.drawItem(stack, itemX, itemY, scale.get(), true);
                }
            }
        }
    }

    private ItemStack getStack(int i) {
        return mc.player.getInventory().getStack(i);
    }

    private void drawBackground(HudRenderer renderer, int x, int y) {
        int w = getWidth();
        int h = getHeight();

        switch (background.get()) {
            case Texture, Outline ->
                renderer.texture(background.get() == Background.Texture ? TEXTURE : TEXTURE_TRANSPARENT, x, y, w, h, color.get());
            case Flat -> renderer.quad(x, y, w, h, color.get());
        }
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
