/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class InventoryViewerHud extends HudModule {
    public enum Background {
        None,
        Light,
        LightTransparent,
        Dark,
        DarkTransparent,
        Flat
    }

    private static final Identifier TEXTURE_LIGHT = new Identifier("meteor-client", "container_3x9.png");
    private static final Identifier TEXTURE_LIGHT_TRANSPARENT = new Identifier("meteor-client", "container_3x9-transparent.png");
    private static final Identifier TEXTURE_DARK = new Identifier("meteor-client", "container_3x9-dark.png");
    private static final Identifier TEXTURE_DARK_TRANSPARENT = new Identifier("meteor-client", "container_3x9-dark-transparent.png");

    public InventoryViewerHud(HUD hud) {
        super(hud, "inventory-viewer", "Displays your inventory.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(176 * hud.invViewerScale.get(), 67 * hud.invViewerScale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        int x = box.getX();
        int y = box.getY();

        drawBackground(x, y);

        if (mc.player != null) {
            for (int row = 0; row < 3; row++) {
                for (int i = 0; i < 9; i++) {
                    RenderUtils.drawItem(mc.player.inventory.getStack(9 + row * 9 + i), (int) (x / hud.invViewerScale.get() + 8 + i * 18), (int) (y / hud.invViewerScale.get() + 7 + row * 18), hud.invViewerScale.get(), true);
                }
            }
        }
    }

    private void drawBackground(int x, int y) {
        switch(hud.invViewerBackground.get()) {
            case Light:
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(TEXTURE_LIGHT);
                DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, box.width,  box.height, box.height, box.width);
                break;
            case LightTransparent:
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(TEXTURE_LIGHT_TRANSPARENT);
                DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, box.width,  box.height, box.height, box.width);
                break;
            case Dark:
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(TEXTURE_DARK);
                DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, box.width,  box.height, box.height, box.width);
                break;
            case DarkTransparent:
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(TEXTURE_DARK_TRANSPARENT);
                DrawableHelper.drawTexture(Matrices.getMatrixStack(), x, y, 0, 0, 0, box.width,  box.height, box.height, box.width);
                break;
            case Flat:
                Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
                Renderer.NORMAL.quad(x, y, box.width, box.height, hud.invViewerColor.get());
                Renderer.NORMAL.end();
                break;
        }
    }
}
