/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.events.entity;

import minegame159.meteorclient.events.Cancellable;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

public class RenderLivingEntityEvent extends Cancellable {
    public LivingEntity entity;

    public static class Pre extends RenderLivingEntityEvent {
        private static final Pre INSTANCE = new Pre();

        public static Pre get(LivingEntity entity) {
            INSTANCE.entity = entity;
            return INSTANCE;
        }
    }

    public static class Post extends RenderLivingEntityEvent {
        private static final Post INSTANCE = new Post();

        public static Post get(LivingEntity entity) {
            INSTANCE.entity = entity;
            return INSTANCE;
        }
    }

    public static class Invoke extends RenderLivingEntityEvent {
        private static final Invoke INSTANCE = new Invoke();

        public EntityModel<LivingEntity> model;
        public MatrixStack matrices;
        public VertexConsumer vertices;
        public int light, overlay;
        public float red, green, blue, alpha;

        public static Invoke get(EntityModel<LivingEntity> model, MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha, LivingEntity entity) {
            INSTANCE.model = model;
            INSTANCE.matrices = matrices;
            INSTANCE.vertices = vertices;
            INSTANCE.light = light;
            INSTANCE.overlay = overlay;
            INSTANCE.red = red;
            INSTANCE.green = green;
            INSTANCE.blue = blue;
            INSTANCE.alpha = alpha;
            INSTANCE.entity = entity;
            return INSTANCE;
        }
    }
}
