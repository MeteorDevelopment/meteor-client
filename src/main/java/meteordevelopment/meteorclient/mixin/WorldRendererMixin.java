/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.mixininterface.IWorldRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.postprocess.EntityShader;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements IWorldRenderer {
    @Unique private ESP esp;

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Inject(method = "checkEmpty", at = @At("HEAD"), cancellable = true)
    private void onCheckEmpty(MatrixStack matrixStack, CallbackInfo info) {
        info.cancel();
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, Entity entity, double cameraX, double cameraY, double cameraZ, BlockPos pos, BlockState state, int i, CallbackInfo ci) {
        if (Modules.get().isActive(BlockSelection.class)) ci.cancel();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"), index = 3)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return Modules.get().isActive(Freecam.class) || spectator;
    }

    // No Render

    @WrapWithCondition(method = "method_62216", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WeatherRendering;renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V"))
    private boolean shouldRenderPrecipitation(WeatherRendering instance, World world, VertexConsumerProvider vertexConsumers, int ticks, float tickProgress, Vec3d pos) {
        return !Modules.get().get(NoRender.class).noWeather();
    }

    @WrapWithCondition(method = "method_62216", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldBorderRendering;render(Lnet/minecraft/world/border/WorldBorder;Lnet/minecraft/util/math/Vec3d;DD)V"))
    private boolean shouldRenderWorldBorder(WorldBorderRendering instance, WorldBorder border, Vec3d cameraPos, double viewDistanceBlocks, double farPlaneDistance) {
        return !Modules.get().get(NoRender.class).noWorldBorder();
    }

	@Inject(method = "hasBlindnessOrDarkness(Lnet/minecraft/client/render/Camera;)Z", at = @At("HEAD"), cancellable = true)
	private void hasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> info) {
		if (Modules.get().get(NoRender.class).noBlindness() || Modules.get().get(NoRender.class).noDarkness()) info.setReturnValue(null);
	}

    // Entity Shaders

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        PostProcessShaders.beginRender();
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        draw(entity, cameraX, cameraY, cameraZ, tickDelta, vertexConsumers, matrices, PostProcessShaders.CHAMS, Color.WHITE);
        draw(entity, cameraX, cameraY, cameraZ, tickDelta, vertexConsumers, matrices, PostProcessShaders.ENTITY_OUTLINE, Modules.get().get(ESP.class).getColor(entity));
    }

    @Unique
    private void draw(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, VertexConsumerProvider vertexConsumers, MatrixStack matrices, EntityShader shader, Color color) {
        if (shader.shouldDraw(entity) && !PostProcessShaders.isCustom(vertexConsumers) && color != null) {
            meteor$pushEntityOutlineFramebuffer(shader.framebuffer);
            PostProcessShaders.rendering = true;

            shader.vertexConsumerProvider.setColor(color.r, color.g, color.b, color.a);
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, shader.vertexConsumerProvider);

            PostProcessShaders.rendering = false;
            meteor$popEntityOutlineFramebuffer();
        }
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onRender(CallbackInfo ci) {
        PostProcessShaders.endRender();
    }

    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;setColor(IIII)V"))
    private void setGlowColor(OutlineVertexConsumerProvider instance, int red, int green, int blue, int alpha, Operation<Void> original, @Local LocalRef<Entity> entity) {
        if (!getESP().isGlow() || getESP().shouldSkip(entity.get())) original.call(instance, red, green, blue, alpha);
        else {
            Color color = getESP().getColor(entity.get());

            if (color == null) original.call(instance, red, green, blue, alpha);
            else instance.setColor(color.r, color.g, color.b, color.a);
        }
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int width, int height, CallbackInfo info) {
        PostProcessShaders.onResized(width, height);
    }

    // Fullbright

    @ModifyVariable(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At(value = "STORE"), ordinal = 0)
    private static int getLightmapCoordinatesModifySkyLight(int sky) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightType.SKY), sky);
    }

    @ModifyVariable(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At(value = "STORE"), ordinal = 1)
    private static int getLightmapCoordinatesModifyBlockLight(int sky) {
        return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightType.BLOCK), sky);
    }

    @Unique
    private ESP getESP() {
        if (esp == null) {
            esp = Modules.get().get(ESP.class);
        }

        return esp;
    }

    // IWorldRenderer

    @Shadow
    private Framebuffer entityOutlineFramebuffer;

    @Shadow
    @Final
    private DefaultFramebufferSet framebufferSet;

    @Unique
    private Stack<Framebuffer> framebufferStack;

    @Unique
    private Stack<Handle<Framebuffer>> framebufferHandleStack;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$IWorldRenderer(CallbackInfo info) {
        framebufferStack = new ObjectArrayList<>();
        framebufferHandleStack = new ObjectArrayList<>();
    }

    @Override
    public void meteor$pushEntityOutlineFramebuffer(Framebuffer framebuffer) {
        framebufferStack.push(this.entityOutlineFramebuffer);
        this.entityOutlineFramebuffer = framebuffer;

        framebufferHandleStack.push(this.framebufferSet.entityOutlineFramebuffer);
        this.framebufferSet.entityOutlineFramebuffer = () -> framebuffer;
    }

    @Override
    public void meteor$popEntityOutlineFramebuffer() {
        this.entityOutlineFramebuffer = framebufferStack.pop();
        this.framebufferSet.entityOutlineFramebuffer = framebufferHandleStack.pop();
    }
}
