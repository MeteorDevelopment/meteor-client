/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityTooltipComponent implements MeteorTooltipData, ClientTooltipComponent {
    protected final LivingEntity entity;
    private static double spin;

    public EntityTooltipComponent(LivingEntity entity) {
        this.entity = entity;
    }

    @Override
    public ClientTooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(Font textRenderer) {
        return 48;
    }

    @Override
    public int getWidth(Font textRenderer) {
        return 64;
    }

    @Override
    public void renderImage(Font textRenderer, int x, int y, int width, int height, GuiGraphics context) {
        var state = (LivingEntityRenderState) mc.getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 1);

        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;

        state.bodyRot = (float) (spin % 360);
        state.yRot = 0;
        state.xRot = 0;

        x += (width - getWidth(null)) / 2;
        y += 4;

        width = getWidth(null);
        height = getHeight(null);

        float scale = Math.max(width, height) / 2f * 1.25f;
        Vector3f translation = new Vector3f(0, 0.1f, 0);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);

        context.submitEntityRenderState(state, scale, translation, rotation, null, x, y, x + width, y + height);
        spin += 3 * mc.getDeltaTracker().getGameTimeDeltaTicks();
    }
}
