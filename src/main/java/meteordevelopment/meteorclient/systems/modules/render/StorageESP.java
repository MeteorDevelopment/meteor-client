/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.MeshBuilderVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.SimpleBlockRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShader;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StorageESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOpened = settings.createGroup("Opened Rendering");
    private final Set<BlockPos> interactedBlocks = new HashSet<>();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Rendering mode.")
        .defaultValue(Mode.Shader)
        .build()
    );

    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
        .name("storage-blocks")
        .description("Select the storage blocks to display.")
        .defaultValue(StorageBlockListSetting.STORAGE_BLOCKS)
        .build()
    );
    
    // 新增：箱子矿车开关
    private final Setting<Boolean> chestMinecarts = sgGeneral.add(new BoolSetting.Builder()
        .name("chest-minecarts")
        .description("Display chest minecarts.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draws tracers to storage blocks.")
        .defaultValue(false)
        .build()
    );

    public final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    public final Setting<Integer> fillOpacity = sgGeneral.add(new IntSetting.Builder()
        .name("fill-opacity")
        .description("The opacity of the shape fill.")
        .visible(() -> shapeMode.get() != ShapeMode.Lines)
        .defaultValue(50)
        .range(0, 255)
        .sliderMax(255)
        .build()
    );

    public final Setting<Integer> outlineWidth = sgGeneral.add(new IntSetting.Builder()
        .name("width")
        .description("The width of the shader outline.")
        .visible(() -> mode.get() == Mode.Shader)
        .defaultValue(1)
        .range(1, 10)
        .sliderRange(1, 5)
        .build()
    );

    public final Setting<Double> glowMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("glow-multiplier")
        .description("Multiplier for glow effect")
        .visible(() -> mode.get() == Mode.Shader)
        .decimalPlaces(3)
        .defaultValue(3.5)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<SettingColor> chest = sgGeneral.add(new ColorSetting.Builder()
        .name("chest")
        .description("The color of chests.")
        .defaultValue(new SettingColor(255, 160, 0, 255))
        .build()
    );

    private final Setting<SettingColor> trappedChest = sgGeneral.add(new ColorSetting.Builder()
        .name("trapped-chest")
        .description("The color of trapped chests.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private final Setting<SettingColor> barrel = sgGeneral.add(new ColorSetting.Builder()
        .name("barrel")
        .description("The color of barrels.")
        .defaultValue(new SettingColor(255, 160, 0, 255))
        .build()
    );

    private final Setting<SettingColor> shulker = sgGeneral.add(new ColorSetting.Builder()
        .name("shulker")
        .description("The color of Shulker Boxes.")
        .defaultValue(new SettingColor(255, 160, 0, 255))
        .build()
    );

    private final Setting<SettingColor> enderChest = sgGeneral.add(new ColorSetting.Builder()
        .name("ender-chest")
        .description("The color of Ender Chests.")
        .defaultValue(new SettingColor(120, 0, 255, 255))
        .build()
    );

    private final Setting<SettingColor> other = sgGeneral.add(new ColorSetting.Builder()
        .name("other")
        .description("The color of furnaces, dispensers, droppers and hoppers.")
        .defaultValue(new SettingColor(140, 140, 140, 255))
        .build()
    );

    private final Setting<Double> fadeDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fade-distance")
        .description("The distance at which the color will fade.")
        .defaultValue(6)
        .min(0)
        .sliderMax(12)
        .build()
    );

    private final Setting<Boolean> hideOpened = sgOpened.add(new BoolSetting.Builder()
        .name("hide-opened")
        .description("Hides opened containers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> openedColor = sgOpened.add(new ColorSetting.Builder()
        .name("opened-color")
        .description("Optional setting to change colors of opened chests, as opposed to not rendering. Disabled at zero opacity.")
        .defaultValue(new SettingColor(203, 90, 203, 0))
        .build()
    );

    private Color lineColor = new Color(0, 0, 0, 0);
    private Color sideColor = new Color(0, 0, 0, 0);
    
    private int count;

    private final MeshBuilder mesh;
    private final MeshBuilderVertexConsumerProvider vertexConsumerProvider;
    
    // 优化：缓存常用设置，减少重复get()调用
    private List<BlockEntityType<?>> cachedStorageBlocks;
    private boolean cachedHideOpened;
    private int cachedFillOpacity;
    private double cachedFadeDistance;
    private boolean cachedTracers;
    private ShapeMode cachedShapeMode;
    private boolean cachedChestMinecarts;

    public StorageESP() {
        super(Categories.Render, "storage-esp", "Renders all specified storage blocks and minecarts.");

        mesh = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED);
        vertexConsumerProvider = new MeshBuilderVertexConsumerProvider(mesh);
    }
    
    private boolean getBlockEntityColor(BlockEntity blockEntity) {
        // 优化：快速检查方块类型是否在存储列表中
        if (!cachedStorageBlocks.contains(blockEntity.getType())) return false;

        // 优化：使用 instanceof 进行快速类型检查
        if (blockEntity instanceof ChestBlockEntity) {
            lineColor.set(chest.get());
        } else if (blockEntity instanceof EnderChestBlockEntity) {
            lineColor.set(enderChest.get());
        } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
            lineColor.set(shulker.get());
        } else if (blockEntity instanceof BarrelBlockEntity) {
            lineColor.set(barrel.get());
        } else if (blockEntity instanceof TrappedChestBlockEntity) {
            lineColor.set(trappedChest.get());
        } else if (blockEntity instanceof AbstractFurnaceBlockEntity 
              || blockEntity instanceof DispenserBlockEntity 
              || blockEntity instanceof HopperBlockEntity
              || blockEntity instanceof BrewingStandBlockEntity 
              || blockEntity instanceof ChiseledBookshelfBlockEntity 
              || blockEntity instanceof CrafterBlockEntity 
              || blockEntity instanceof DecoratedPotBlockEntity) {
            lineColor.set(other.get());
        } 
        else {
            return false;
        }

        return true;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WButton clear = list.add(theme.button("Clear Rendering Cache")).expandX().widget();
        clear.action = interactedBlocks::clear;
        return list;
    }

    @EventHandler
    private void onBlockInteract(InteractBlockEvent event) {
        BlockPos pos = event.result.getBlockPos();
        if (interactedBlocks.contains(pos)) return;

        BlockEntity blockEntity = mc.world.getBlockEntity(pos);
        if (blockEntity == null) return;

        interactedBlocks.add(pos);
        if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
            BlockState state = chestBlockEntity.getCachedState();
            if (state.contains(ChestBlock.CHEST_TYPE)) {
                ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
                if (chestType != ChestType.SINGLE && state.contains(ChestBlock.FACING)) {
                    Direction facing = state.get(ChestBlock.FACING);
                    BlockPos otherPartPos = pos.offset(chestType == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
                    interactedBlocks.add(otherPartPos);
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        count = 0;
        
        // 优化：缓存常用设置，减少重复get()调用
        cachedStorageBlocks = storageBlocks.get();
        cachedHideOpened = hideOpened.get();
        cachedFillOpacity = fillOpacity.get();
        cachedFadeDistance = fadeDistance.get();
        cachedTracers = tracers.get();
        cachedShapeMode = shapeMode.get();
        cachedChestMinecarts = chestMinecarts.get();
        
        boolean isShader = mode.get() == Mode.Shader;
        double fadeDistSq = cachedFadeDistance * cachedFadeDistance;
        int alphaThreshold = (int)(0.075 * 255); // 预计算透明度阈值

        // 1. 渲染方块实体 (箱子、桶等)
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            boolean interacted = interactedBlocks.contains(blockEntity.getPos());
            if (interacted && cachedHideOpened) continue;

            if (!getBlockEntityColor(blockEntity)) continue;

            if (interacted && openedColor.get().a > 0) {
                lineColor.set(openedColor.get());
            }

            if (cachedShapeMode == ShapeMode.Sides || cachedShapeMode == ShapeMode.Both) {
                sideColor.set(lineColor);
                sideColor.a = cachedFillOpacity;
            }

            // 优化：预计算位置和使用更高效的距离计算
            double blockX = blockEntity.getPos().getX() + 0.5;
            double blockY = blockEntity.getPos().getY() + 0.5;
            double blockZ = blockEntity.getPos().getZ() + 0.5;
            double dist = PlayerUtils.squaredDistanceTo(blockX, blockY, blockZ);
            
            // 优化：更高效的透明度计算
            double alphaFactor;
            if (dist > fadeDistSq) {
                alphaFactor = 1.0;
            } else {
                alphaFactor = dist / fadeDistSq;
                if (alphaFactor < 0.075) continue; // 早期退出
            }

            // 懒加载开启 Mesh
            if (count == 0 && isShader) {
                mesh.begin();
            }

            int prevLineA = lineColor.a;
            int prevSideA = sideColor.a;
            lineColor.a = (int)(lineColor.a * alphaFactor);
            sideColor.a = (int)(sideColor.a * alphaFactor);

            if (cachedTracers) {
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, 
                    blockX, blockY, blockZ, lineColor);
            }

            if (mode.get() == Mode.Box) {
                renderBox(event, blockEntity);
            } else if (isShader) {
                renderShader(event, blockEntity);
            }

            lineColor.a = prevLineA;
            sideColor.a = prevSideA;

            count++;
        }

        // 2. 渲染箱子矿车
        if (cachedChestMinecarts && mc.world != null) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ChestMinecartEntity minecart) {
                    lineColor.set(chest.get());

                    if (cachedShapeMode == ShapeMode.Sides || cachedShapeMode == ShapeMode.Both) {
                        sideColor.set(lineColor);
                        sideColor.a = cachedFillOpacity;
                    }

                    // 优化：直接计算距离，避免重复调用
                    double dist = PlayerUtils.squaredDistanceTo(minecart.getX(), minecart.getY(), minecart.getZ());
                    
                    // 优化：相同的透明度计算优化
                    double alphaFactor;
                    if (dist > fadeDistSq) {
                        alphaFactor = 1.0;
                    } else {
                        alphaFactor = dist / fadeDistSq;
                        if (alphaFactor < 0.075) continue; // 早期退出
                    }

                    // 懒加载开启 Mesh (如果之前方块实体循环没开过)
                    if (count == 0 && isShader) {
                        mesh.begin();
                    }

                    int prevLineA = lineColor.a;
                    int prevSideA = sideColor.a;
                    lineColor.a = (int)(lineColor.a * alphaFactor);
                    sideColor.a = (int)(sideColor.a * alphaFactor);

                    if (cachedTracers) {
                        event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, 
                            minecart.getX(), minecart.getY(), minecart.getZ(), lineColor);
                    }

                    // 矿车 Box 渲染 (透视效果由 Meteor 渲染管线保证)
                    double x = MathHelper.lerp(event.tickDelta, minecart.lastRenderX, minecart.getX()) - minecart.getX();
                    double y = MathHelper.lerp(event.tickDelta, minecart.lastRenderY, minecart.getY()) - minecart.getY();
                    double z = MathHelper.lerp(event.tickDelta, minecart.lastRenderZ, minecart.getZ()) - minecart.getZ();
                    
                    Box box = minecart.getBoundingBox();
                    event.renderer.box(x + box.minX, y + box.minY, z + box.minZ, x + box.maxX, y + box.maxY, z + box.maxZ, sideColor, lineColor, cachedShapeMode, 0);

                    lineColor.a = prevLineA;
                    sideColor.a = prevSideA;

                    count++;
                }
            }
        }

      if (mode.get() == Mode.Shader && count > 0) {
            MeshRenderer.begin()
                .attachments(PostProcessShaders.STORAGE_OUTLINE.framebuffer)
                .clearColor(Color.CLEAR)
                .pipeline(MeteorRenderPipelines.WORLD_COLORED)
                .mesh(mesh, event.matrices)
                .end();

            PostProcessShaders.STORAGE_OUTLINE.render();
        }
    }
    
    private void renderBox(Render3DEvent event, BlockEntity blockEntity) {
        double x1 = blockEntity.getPos().getX();
        double y1 = blockEntity.getPos().getY();
        double z1 = blockEntity.getPos().getZ();

        double x2 = x1 + 1;
        double y2 = y1 + 1;
        double z2 = z1 + 1;

        int excludeDir = 0;
        if (blockEntity instanceof ChestBlockEntity) {
            BlockState state = mc.world.getBlockState(blockEntity.getPos());
            if ((state.getBlock() == Blocks.CHEST || state.getBlock() == Blocks.TRAPPED_CHEST) && state.contains(ChestBlock.CHEST_TYPE) && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                if (state.contains(ChestBlock.FACING)) {
                    excludeDir = Dir.get(ChestBlock.getFacing(state));
                }
            }
        }

        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity) {
            double a = 1.0 / 16.0;

            if (Dir.isNot(excludeDir, Dir.WEST)) x1 += a;
            if (Dir.isNot(excludeDir, Dir.NORTH)) z1 += a;

            if (Dir.isNot(excludeDir, Dir.EAST)) x2 -= a;
            y2 -= a * 2;
            if (Dir.isNot(excludeDir, Dir.SOUTH)) z2 -= a;
        }

        event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, cachedShapeMode, excludeDir);
    }

    private void renderShader(Render3DEvent event, BlockEntity blockEntity) {
        vertexConsumerProvider.setColor(lineColor);
        SimpleBlockRenderer.renderWithBlockEntity(blockEntity, event.tickDelta, vertexConsumerProvider);
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    public boolean isShader() {
        return isActive() && mode.get() == Mode.Shader;
    }

    public enum Mode {
        Box,
        Shader
    }
}