/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.hud.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.hud.ElementRegister;
import minegame159.meteorclient.systems.hud.HudRenderer;
import minegame159.meteorclient.systems.hud.ScaleableHudElement;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

@ElementRegister(name = "inventory-viewer")
public class InventoryViewerHud extends ScaleableHudElement {

    private static final Identifier TEXTURE = new Identifier("meteor-client", "textures/container.png");
    private static final Identifier TEXTURE_TRANSPARENT = new Identifier("meteor-client", "textures/container-transparent.png");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    private final ItemStack[] editorInv;

    public InventoryViewerHud() {
        super("inventory-viewer", "Displays your inventory.");

        editorInv = new ItemStack[9 * 3];
        editorInv[0] = Items.TOTEM_OF_UNDYING.getDefaultStack();
        editorInv[5] = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 6);
        editorInv[19] = new ItemStack(Items.OBSIDIAN, 64);
        editorInv[editorInv.length - 1] = Items.NETHERITE_AXE.getDefaultStack();
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(176 * getScale(), 67 * getScale());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (background.get() != Background.None) drawBackground((int) x, (int) y);

        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = getStack(9 + row * 9 + i);
                if (stack == null) continue;

                RenderUtils.drawItem(stack, (int) (x + (8 + i * 18) * getScale()), (int) (y + (7 + row * 18) * getScale()), getScale(), true);
            }
        }
    }

    private ItemStack getStack(int i) {
        if (isInEditor()) return editorInv[i - 9];
        return mc.player.inventory.getStack(i);
    }

    private void drawBackground(int x, int y) {
        int w = (int) box.width;
        int h = (int) box.height;

        switch(background.get()) {
            case Texture:
            case Outline:
                RenderSystem.color4f(color.get().r / 255F, color.get().g / 255F, color.get().b / 255F, color.get().a / 255F);
                mc.getTextureManager().bindTexture(background.get() == Background.Texture ? TEXTURE : TEXTURE_TRANSPARENT);
                DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, w, h, h, w);
                break;
            case Flat:
                Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
                Renderer.NORMAL.quad(x, y, w, h, color.get());
                Renderer.NORMAL.end();
                break;
        }
    }

    public enum Background {
        None,
        Texture,
        Outline,
        Flat
    }

}
