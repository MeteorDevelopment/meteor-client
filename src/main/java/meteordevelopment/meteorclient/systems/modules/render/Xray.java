/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.MixinPlugin;
import meteordevelopment.meteorclient.events.render.RenderBlockEntityEvent;
import meteordevelopment.meteorclient.events.world.AmbientOcclusionEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.mixin.BlockEntityRenderStateAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

public class Xray extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public static final List<Block> ORES = List.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS);

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("Which blocks to show x-rayed.")
        .defaultValue(ORES)
        .onChanged(_ -> {
            if (isActive()) mc.levelRenderer.allChanged();
        })
        .build()
    );

    public final Setting<Integer> opacity = sgGeneral.add(new IntSetting.Builder()
        .name("opacity")
        .description("The opacity for all other blocks.")
        .defaultValue(25)
        .range(0, 255)
        .sliderMax(255)
        .onChanged(_ -> {
            if (isActive()) mc.levelRenderer.allChanged();
        })
        .build()
    );

    private final Setting<FluidOpacity> fluidOpacity = sgGeneral.add(new EnumSetting.Builder<FluidOpacity>()
        .name("fluid-opacity")
        .description("Which fluids should use xray opacity.")
        .defaultValue(FluidOpacity.Both)
        .onChanged(_ -> {
            if (isActive()) mc.levelRenderer.allChanged();
        })
        .build()
    );

    private final Setting<Boolean> exposedOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("exposed-only")
        .description("Show only exposed ores.")
        .defaultValue(false)
        .onChanged(_ -> {
            if (isActive()) mc.levelRenderer.allChanged();
        })
        .build());

    public Xray() {
        super(Categories.Render, "xray", "Only renders specified blocks. Good for mining.");
    }

    @Override
    public void onActivate() {
        mc.levelRenderer.allChanged();
    }

    @Override
    public void onDeactivate() {
        mc.levelRenderer.allChanged();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        if (MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse())
            return theme.label("Warning: Due to shaders in use, opacity is overridden to 0.");

        return null;
    }

    @EventHandler
    private void onRenderBlockEntity(RenderBlockEntityEvent event) {
        BlockState state = ((BlockEntityRenderStateAccessor) event.blockEntityState).meteor$getBlockState();
        if (getAlpha(state, event.blockEntityState.blockPos) == 0) event.cancel();
    }

    @EventHandler
    private void onChunkOcclusion(ChunkOcclusionEvent event) {
        event.cancel();
    }

    @EventHandler
    private void onAmbientOcclusion(AmbientOcclusionEvent event) {
        event.lightLevel = 1;
    }

    public boolean modifyDrawSide(BlockState state, BlockGetter view, BlockPos pos, Direction facing, boolean returns) {
        if (!returns && !isBlocked(state.getBlock(), pos)) {
            BlockPos adjPos = pos.relative(facing);
            BlockState adjState = view.getBlockState(adjPos);
            return adjState.getFaceOcclusionShape(facing.getOpposite()) != Shapes.block() || adjState.getBlock() != state.getBlock() || !adjState.isSolidRender() || isBlocked(adjState.getBlock(), adjPos);
        }

        return returns;
    }

    public boolean isBlocked(Block block, BlockPos blockPos) {
        return !(blocks.get().contains(block) && (!exposedOnly.get() || (blockPos == null || BlockUtils.isExposed(blockPos))));
    }

    public static int getAlpha(BlockState state, BlockPos pos) {
        WallHack wallHack = Modules.get().get(WallHack.class);
        Xray xray = Modules.get().get(Xray.class);
        Block block = state.getBlock();

        if (wallHack.isActive() && wallHack.blocks.get().contains(block)) {
            if (MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse()) return 0;

            int alpha;

            if (xray.isActive()) alpha = xray.opacity.get();
            else alpha = wallHack.opacity.get();

            return alpha;
        } else if (xray.isActive() && !wallHack.isActive() && xray.isBlocked(block, pos)) {
            return (MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse()) ? 0 : xray.opacity.get();
        }

        return -1;
    }

    public static int getFluidAlpha(FluidState state, BlockPos pos) {
        WallHack wallHack = Modules.get().get(WallHack.class);
        Xray xray = Modules.get().get(Xray.class);
        Block fluidBlock = state.createLegacyBlock().getBlock();

        if (wallHack.isActive() && wallHack.blocks.get().contains(fluidBlock)) {
            if (MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse()) return 0;

            return xray.isActive() ? xray.opacity.get() : wallHack.opacity.get();
        } else if (xray.isActive() && !wallHack.isActive() && xray.shouldApplyFluidOpacity(state) && xray.isBlocked(fluidBlock, pos)) {
            return (MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse()) ? 0 : xray.opacity.get();
        }

        return -1;
    }

    private boolean shouldApplyFluidOpacity(FluidState state) {
        return switch (fluidOpacity.get()) {
            case None -> false;
            case Water -> state.is(FluidTags.WATER);
            case Lava -> state.is(FluidTags.LAVA);
            case Both -> state.is(FluidTags.WATER) || state.is(FluidTags.LAVA);
        };
    }

    public enum FluidOpacity {
        None,
        Water,
        Lava,
        Both
    }
}
