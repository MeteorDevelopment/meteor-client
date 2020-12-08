/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.ShapeBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

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
        box.setSize(176 * hud.invViewerScale(), 67 * hud.invViewerScale());
    }

    @Override
    public void render(HudRenderer renderer) {
        MinecraftClient mc = MinecraftClient.getInstance();

        int x = box.getX();
        int y = box.getY();

        drawBackground(x, y);

        if (mc.player != null) {
            DiffuseLighting.enable();
            RenderSystem.pushMatrix();
            RenderSystem.scaled(hud.invViewerScale(), hud.invViewerScale(), 1);

            for (int row = 0; row < 3; row++) {
                for (int i = 0; i < 9; i++) {
                    drawItem(mc.player.inventory.getStack(9 + row * 9 + i), (int) (x / hud.invViewerScale() + 8 + i * 18), (int) (y / hud.invViewerScale() + 7 + row * 18));
                }
            }

            RenderSystem.popMatrix();
            DiffuseLighting.disable();
        }
    }

    private void drawItem(ItemStack itemStack, int x, int y) {
        MinecraftClient mc = MinecraftClient.getInstance();

        mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
        mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, null);
    }

    private void drawBackground(int x, int y) {
        MinecraftClient mc = MinecraftClient.getInstance();

        switch(hud.invViewerBackground()) {
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
                ShapeBuilder.begin(null, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
                ShapeBuilder.quad(x, y, box.width, box.height, hud.invViewerColor());
                ShapeBuilder.end();
                break;
        }
    }
}
