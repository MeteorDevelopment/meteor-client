/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.biome.DryFoliageColors;
import net.minecraft.world.biome.FoliageColors;
import net.minecraft.world.biome.GrassColors;
import net.minecraft.world.chunk.light.LightingProvider;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class SimpleBlockRenderer {
    private static final boolean FABRIC_FLUID_RENDERER = FabricLoader.getInstance().isModLoaded("fabric-rendering-fluids-v1");
    private static final List<BlockModelPart> PARTS = new ArrayList<>();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Random RANDOM = Random.create();
    private static final Vector3f POS = new Vector3f();

    private static final OrderedRenderCommandQueueImpl renderCommandQueue = new OrderedRenderCommandQueueImpl();

    private static VertexConsumerProvider provider;

    private static final RenderDispatcher renderDispatcher = new RenderDispatcher(
        renderCommandQueue,
        mc.getBlockRenderManager(),
        new WrapperImmediateVertexConsumerProvider(() -> provider),
        mc.getAtlasManager(),
        NoopOutlineVertexConsumerProvider.INSTANCE,
        NoopImmediateVertexConsumerProvider.INSTANCE,
        mc.textRenderer
    );

    private SimpleBlockRenderer() {}

    public static void renderFlat(@Nullable BlockRenderView renderView, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, MatrixStack matrices, float tickDelta, VertexConsumerProvider vertexConsumerProvider) {
        matrices.push();
        matrices.translate(pos.getX(), pos.getY(), pos.getZ());

        if (renderView == null) {
            renderView = new StaticBlockRenderView(pos, state);
        }

        // Render block model
        if (state.getRenderType() == BlockRenderType.MODEL) {
            VertexConsumer consumer = vertexConsumerProvider.getBuffer(RenderLayers.solid());

            BlockStateModel model = mc.getBlockRenderManager().getModel(state);
            RANDOM.setSeed(state.getRenderingSeed(pos));
            model.addParts(RANDOM, PARTS);

            matrices.translate(state.getModelOffset(pos));
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();

            for (BlockModelPart part : PARTS) {
                for (Direction direction : DIRECTIONS) {
                    List<BakedQuad> quads = part.getQuads(direction);
                    if (!quads.isEmpty()) renderQuads(quads, matrix4f, consumer);
                }

                List<BakedQuad> quads = part.getQuads(null);
                if (!quads.isEmpty()) renderQuads(quads, matrix4f, consumer);
            }

            PARTS.clear();
        }

        // Render fluid
        if (!state.getFluidState().isEmpty()) {
            VertexConsumer consumer = vertexConsumerProvider.getBuffer(RenderLayers.solid());
            renderFluid(renderView, pos, state, consumer);
        }

        // Render block entity
        if (blockEntity != null || state.getBlock() instanceof BlockWithEntity) {
            if (blockEntity == null && state.getBlock() instanceof BlockWithEntity blockWithEntity) {
                blockEntity = blockWithEntity.createBlockEntity(pos, state);
            }

            if (blockEntity != null) {
                BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = mc.getBlockEntityRenderDispatcher().get(blockEntity);
                if (renderer != null && blockEntity.getType().supports(blockEntity.getCachedState())) {
                    try {
                        SimpleBlockRenderer.provider = vertexConsumerProvider;

                        BlockEntityRenderState renderState = renderer.createRenderState();
                        renderer.updateRenderState(blockEntity, renderState, tickDelta, mc.gameRenderer.getCamera().getCameraPos(), null);
                        renderer.render(renderState, matrices, renderCommandQueue, mc.gameRenderer.getEntityRenderStates().cameraRenderState);

                        renderDispatcher.render();
                        renderCommandQueue.onNextFrame();

                        SimpleBlockRenderer.provider = null;
                    } catch (Throwable t) {
                        MeteorClient.LOG.error("Oops! no render", t);
                    }
                }
            }
        }

        matrices.pop();
    }

    public static void renderFull(@Nullable BlockRenderView renderView, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, MatrixStack matrices, float tickDelta, VertexConsumerProvider.Immediate vertexConsumerProvider) {
        matrices.push();
        matrices.translate(pos.getX(), pos.getY(), pos.getZ());

        if (renderView == null) {
            renderView = new StaticBlockRenderView(pos, state);
        }

        // Render block model
        if (state.getRenderType() == BlockRenderType.MODEL) {
            VertexConsumer consumer = vertexConsumerProvider.getBuffer(RenderLayers.cutout());

            BlockStateModel model = mc.getBlockRenderManager().getModel(state);
            RANDOM.setSeed(42L);
            model.addParts(RANDOM, PARTS);

            MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer().render(
                renderView,
                PARTS,
                state,
                pos,
                matrices,
                consumer,
                false,
                OverlayTexture.DEFAULT_UV
            );

            PARTS.clear();
        }

        // Render fluid
        if (!state.getFluidState().isEmpty()) {
            VertexConsumer consumer = vertexConsumerProvider.getBuffer(RenderLayers.cutout());
            renderFluid(renderView, pos, state, consumer);
        }

        // Render block entity
        if (blockEntity != null || state.getBlock() instanceof BlockWithEntity) {
            if (blockEntity == null && state.getBlock() instanceof BlockWithEntity blockWithEntity) {
                blockEntity = blockWithEntity.createBlockEntity(pos, state);
            }

            if (blockEntity != null) {
                BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = mc.getBlockEntityRenderDispatcher().get(blockEntity);
                if (renderer != null && blockEntity.getType().supports(blockEntity.getCachedState())) {
                    try {
                        RenderDispatcher renderDispatcher = new RenderDispatcher(
                            renderCommandQueue,
                            mc.getBlockRenderManager(),
                            vertexConsumerProvider,
                            mc.getAtlasManager(),
                            NoopOutlineVertexConsumerProvider.INSTANCE,
                            NoopImmediateVertexConsumerProvider.INSTANCE,
                            mc.textRenderer
                        );

                        BlockEntityRenderState renderState = renderer.createRenderState();
                        renderer.updateRenderState(blockEntity, renderState, tickDelta, mc.gameRenderer.getCamera().getCameraPos(), null);
                        renderer.render(renderState, matrices, renderCommandQueue, mc.gameRenderer.getEntityRenderStates().cameraRenderState);

                        renderDispatcher.render();
                        renderCommandQueue.onNextFrame();
                    } catch (Throwable t) {
                        MeteorClient.LOG.error("Oops! no render", t);
                    }
                }
            }
        }

        matrices.pop();
    }

    private static void renderFluid(BlockRenderView renderView, BlockPos pos, BlockState state, VertexConsumer consumer) {
        if (FABRIC_FLUID_RENDERER) {
            FluidRenderHandlerRegistry.INSTANCE.get(state.getFluidState().getFluid()).renderFluid(
                pos,
                renderView,
                consumer,
                state,
                state.getFluidState()
            );
        } else {
            MinecraftClient.getInstance().getBlockRenderManager().renderFluid(
                pos,
                renderView,
                consumer,
                state,
                state.getFluidState()
            );
        }
    }

    private static void renderQuads(List<BakedQuad> quads, Matrix4f matrix4f, VertexConsumer consumer) {
        for (BakedQuad quad : quads) {
            for (int i = 0; i < 4; i++) {
                Vector3fc pos = quad.getPosition(i);
                POS.set(pos.x(), pos.y(), pos.z()).mulPosition(matrix4f);

                consumer.vertex(POS.x(), POS.y(), POS.z());
            }
        }
    }

    public static boolean hasAnimatedTextures(BlockState state) {
        if (!state.getFluidState().isEmpty()) {
            return true;
        }

        BlockStateModel model = mc.getBlockRenderManager().getModel(state);
        RANDOM.setSeed(42L);
        model.addParts(RANDOM, PARTS);

        try {
            for (BlockModelPart part : PARTS) {
                for (Direction direction : DIRECTIONS) {
                    for (BakedQuad quad : part.getQuads(direction)) {
                        if (quad.sprite().getContents().isAnimated()) {
                            return true;
                        }
                    }
                }

                for (BakedQuad quad : part.getQuads(null)) {
                    if (quad.sprite().getContents().isAnimated()) {
                        return true;
                    }
                }
            }

            return false;
        } finally {
            PARTS.clear();
        }
    }

    private record StaticBlockRenderView(BlockPos originPos, BlockState originState) implements BlockRenderView {
        @Override
        public float getBrightness(Direction direction, boolean shaded) {
            if (!shaded) {
                return 1f;
            } else {
                return switch (direction) {
                    case DOWN -> 0.5F;
                    case UP -> 1.0F;
                    case NORTH, SOUTH -> 0.8F;
                    case WEST, EAST -> 0.6F;
                };
            }
        }

        @Override
        public LightingProvider getLightingProvider() {
            return null;
        }

        @Override
        public int getColor(BlockPos pos, ColorResolver color) {
            if (color == BiomeColors.GRASS_COLOR) return GrassColors.getColor(0.7f, 0.8f);
            if (color == BiomeColors.FOLIAGE_COLOR) return FoliageColors.getColor(0.7f, 0.8f);
            if (color == BiomeColors.DRY_FOLIAGE_COLOR) return DryFoliageColors.getColor(0.7f, 0.8f);
            return 0x3f76e4;
        }

        @Override
        public int getLightLevel(LightType type, BlockPos pos) {
            return 8;
        }

        @Override
        public int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
            return 8;
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return pos.equals(this.originPos()) ? this.originState() : Blocks.AIR.getDefaultState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 1;
        }

        @Override
        public int getBottomY() {
            return 0;
        }
    }
}
