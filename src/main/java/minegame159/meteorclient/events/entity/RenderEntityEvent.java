/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.entity;

import minegame159.meteorclient.events.Cancellable;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;

import java.util.List;

public class RenderEntityEvent extends Cancellable {
    public Entity entity;

    public static class Pre extends RenderEntityEvent {
        private static final Pre INSTANCE = new Pre();

        public static Pre get(Entity entity) {
            INSTANCE.entity = entity;
            return INSTANCE;
        }
    }

    public static class Post extends RenderEntityEvent {
        private static final Post INSTANCE = new Post();

        public static Post get(Entity entity) {
            INSTANCE.entity = entity;
            return INSTANCE;
        }
    }

    public static class LiveEntity extends RenderEntityEvent {
        private static final LiveEntity INSTANCE = new LiveEntity();

        public EntityModel<Entity> model;
        public LivingEntity entity;
        public List<FeatureRenderer<LivingEntity, EntityModel<LivingEntity>>> features;
        public MatrixStack matrixStack;
        public VertexConsumerProvider vertexConsumerProvider;
        public int light;
        public float tickDelta, yaw;

        public static LiveEntity get(EntityModel<Entity> model, LivingEntity entity, List<FeatureRenderer<LivingEntity, EntityModel<LivingEntity>>> features, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
            INSTANCE.model = model;
            INSTANCE.entity = entity;
            INSTANCE.features = features;
            INSTANCE.yaw = yaw;
            INSTANCE.tickDelta = tickDelta;
            INSTANCE.matrixStack = matrixStack;
            INSTANCE.vertexConsumerProvider = vertexConsumerProvider;
            INSTANCE.light = light;
            return INSTANCE;
        }
    }

    public static class Crystal extends RenderEntityEvent {
        private static final Crystal INSTANCE = new Crystal();

        public EndCrystalEntity endCrystalEntity;
        public MatrixStack matrixStack;
        public VertexConsumerProvider vertexConsumerProvider;
        public int light;
        public float tickDelta, yaw;
        public ModelPart core, frame, bottom;

        public static Crystal get(EndCrystalEntity endCrystalEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, ModelPart core, ModelPart frame, ModelPart bottom) {
            INSTANCE.endCrystalEntity = endCrystalEntity;
            INSTANCE.yaw = yaw;
            INSTANCE.tickDelta = tickDelta;
            INSTANCE.matrixStack = matrixStack;
            INSTANCE.vertexConsumerProvider = vertexConsumerProvider;
            INSTANCE.light = light;
            INSTANCE.core = core;
            INSTANCE.frame = frame;
            INSTANCE.bottom = bottom;
            return INSTANCE;
        }
    }
}