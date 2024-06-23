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
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.MeshVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.SimpleBlockRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class StorageESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgOpened = settings.createGroup("Opened Rendering");
    private final Set<BlockPos> interactedBlocks = new HashSet<>();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
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
        .description("The color of furnaces, dispenders, droppers and hoppers.")
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
        .defaultValue(new SettingColor(203, 90, 203, 0)) /// TRANSPARENT BY DEFAULT.
        .build()
    );


    private final Color lineColor = new Color(0, 0, 0, 0);
    private final Color sideColor = new Color(0, 0, 0, 0);
    private boolean render;
    private int count;

    private final Mesh mesh;
    private final MeshVertexConsumerProvider vertexConsumerProvider;

    public StorageESP() {
        super(Categories.Render, "storage-esp", "Renders all specified storage blocks.");

        mesh = new ShaderMesh(Shaders.POS_COLOR, DrawMode.Triangles, Mesh.Attrib.Vec3, Mesh.Attrib.Color);
        vertexConsumerProvider = new MeshVertexConsumerProvider(mesh);
    }

    private void getBlockEntityColor(BlockEntity blockEntity) {
        render = false;

        if (!storageBlocks.get().contains(blockEntity.getType())) return;

        if (blockEntity instanceof TrappedChestBlockEntity) lineColor.set(trappedChest.get()); // Must come before ChestBlockEntity as it is the superclass of TrappedChestBlockEntity
        else if (blockEntity instanceof ChestBlockEntity) lineColor.set(chest.get());
        else if (blockEntity instanceof BarrelBlockEntity) lineColor.set(barrel.get());
        else if (blockEntity instanceof ShulkerBoxBlockEntity) lineColor.set(shulker.get());
        else if (blockEntity instanceof EnderChestBlockEntity) lineColor.set(enderChest.get());
        else if (blockEntity instanceof AbstractFurnaceBlockEntity || blockEntity instanceof BrewingStandBlockEntity || blockEntity instanceof ChiseledBookshelfBlockEntity || blockEntity instanceof CrafterBlockEntity || blockEntity instanceof DispenserBlockEntity || blockEntity instanceof DecoratedPotBlockEntity || blockEntity instanceof HopperBlockEntity) lineColor.set(other.get());
        else return;

        render = true;

        if (shapeMode.get() == ShapeMode.Sides || shapeMode.get() == ShapeMode.Both) {
            sideColor.set(lineColor);
            sideColor.a = fillOpacity.get();
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        // Button to Clear Interacted Blocks
        WButton clear = list.add(theme.button("Clear Rendering Cache")).expandX().widget();

        clear.action = () -> {
            interactedBlocks.clear();
        };

        return list;
    }

    @EventHandler
    private void onBlockInteract(InteractBlockEvent event) {
        BlockPos pos = event.result.getBlockPos();
        BlockEntity blockEntity = mc.world.getBlockEntity(pos);

        if (blockEntity == null) return;

        interactedBlocks.add(pos);
        if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
            BlockState state = chestBlockEntity.getCachedState();
            ChestType chestType = state.get(ChestBlock.CHEST_TYPE);

            if (chestType == ChestType.LEFT || chestType == ChestType.RIGHT) {
                // It's part of a double chest
                Direction facing = state.get(ChestBlock.FACING);
                BlockPos otherPartPos = pos.offset(chestType == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());

                interactedBlocks.add(otherPartPos);
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        count = 0;

        if (mode.get() == Mode.Shader) mesh.begin();

        for (BlockEntity blockEntity : Utils.blockEntities()) {
            // Check if the block has been interacted with (opened)
            boolean interacted = interactedBlocks.contains(blockEntity.getPos());
            if (interacted && hideOpened.get()) continue;  // Skip rendering if "hideOpened" is true

            getBlockEntityColor(blockEntity);

            // Set the color to openedColor if its alpha is greater than 0
            if (interacted && openedColor.get().a > 0) {
                // openedColor takes precedence.
                lineColor.set(openedColor.get());
                sideColor.set(openedColor.get());
                sideColor.a = fillOpacity.get(); // Maintain fill opacity setting for consistency
            }

            if (render) {
                double dist = PlayerUtils.squaredDistanceTo(blockEntity.getPos().getX() + 0.5, blockEntity.getPos().getY() + 0.5, blockEntity.getPos().getZ() + 0.5);
                double a = 1;
                if (dist <= fadeDistance.get() * fadeDistance.get()) a = dist / (fadeDistance.get() * fadeDistance.get());

                int prevLineA = lineColor.a;
                int prevSideA = sideColor.a;

                lineColor.a *= a;
                sideColor.a *= a;

                if (tracers.get() && a >= 0.075) {
                    event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, blockEntity.getPos().getX() + 0.5, blockEntity.getPos().getY() + 0.5, blockEntity.getPos().getZ() + 0.5, lineColor);
                }

                if (mode.get() == Mode.Box && a >= 0.075) renderBox(event, blockEntity);

                lineColor.a = prevLineA;
                sideColor.a = prevSideA;

                if (mode.get() == Mode.Shader) renderShader(event, blockEntity);

                count++;
            }
        }
        if (mode.get() == Mode.Shader) PostProcessShaders.STORAGE_OUTLINE.endRender(() -> mesh.render(event.matrices));
    }


    private void renderBox(Render3DEvent event, BlockEntity blockEntity) {
        double x1 = blockEntity.getPos().getX();
        double y1 = blockEntity.getPos().getY();
        double z1 = blockEntity.getPos().getZ();

        double x2 = blockEntity.getPos().getX() + 1;
        double y2 = blockEntity.getPos().getY() + 1;
        double z2 = blockEntity.getPos().getZ() + 1;

        int excludeDir = 0;
        if (blockEntity instanceof ChestBlockEntity) {
            BlockState state = mc.world.getBlockState(blockEntity.getPos());
            if ((state.getBlock() == Blocks.CHEST || state.getBlock() == Blocks.TRAPPED_CHEST) && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                excludeDir = Dir.get(ChestBlock.getFacing(state));
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

        event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode.get(), excludeDir);
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
