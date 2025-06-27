/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityTooltipComponent implements MeteorTooltipData, TooltipComponent {
    protected final LivingEntity entity;
    private static double spin;

    public EntityTooltipComponent(LivingEntity entity) {
        this.entity = entity;
    }

    @Override
    public TooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return 48;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 64;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        var state = (LivingEntityRenderState) mc.getEntityRenderDispatcher().getRenderer(entity).getAndUpdateRenderState(entity, 1);
        state.hitbox = null;

        state.bodyYaw = (float) (spin % 360);
        state.relativeHeadYaw = 0;
        state.pitch = 0;

        x += (width - getWidth(null)) / 2;
        y += 4;

        width = getWidth(null);
        height = getHeight(null);

        float scale = Math.max(width, height) / 2f * 1.25f;
        Vector3f translation = new Vector3f(0, 0.1f, 0);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);

        context.addEntity(state, scale, translation, rotation, null, x, y, x + width, y + height);
        spin += 3 * mc.getRenderTickCounter().getDynamicDeltaTicks();
    }
}
