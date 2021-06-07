/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.world;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.player.AutoTool;
import minegame159.meteorclient.utils.player.FindItemResult;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public class EChestFarmer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("self-toggle")
        .description("Disables when you reach the desired amount of obsidian.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreExisting = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-existing")
        .description("Ignores existing obsidian in your inventory and mines the total target amount.")
        .defaultValue(true)
        .visible(selfToggle::get)
        .build()
    );

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("The amount of obsidian to farm.")
        .defaultValue(64)
        .sliderMax(128)
        .min(8).max(512)
        .visible(selfToggle::get)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the obsidian will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final VoxelShape SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    private BlockPos target;
    private int startCount, prevSlot;

    public EChestFarmer() {
        super(Categories.World, "echest-farmer", "Places and breaks EChests to farm obsidian.");
    }

    @Override
    public void onActivate() {
        target = null;
        startCount = InvUtils.find(Items.OBSIDIAN).getCount();
        prevSlot = mc.player.inventory.selectedSlot;
    }

    @Override
    public void onDeactivate() {
        InvUtils.swap(prevSlot);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Finding target pos
        if (target == null) {
            if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            BlockState state = mc.world.getBlockState(pos);

            if (state.getMaterial().isReplaceable() || state.getBlock() == Blocks.ENDER_CHEST) {
                target = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            }
            else return;
        }

        // Disable if the block is too far away
        if (PlayerUtils.distanceTo(target) > mc.interactionManager.getReachDistance()) {
            error("Target block pos out of reach.");
            target = null;
            return;
        }

        // Toggle if obby amount reached
        if (selfToggle.get() && InvUtils.find(Items.OBSIDIAN).getCount() - (ignoreExisting.get() ? startCount : 0) >= amount.get()) {
            InvUtils.swap(prevSlot);
            toggle();
            return;
        }

        // Break existing echest at target pos
        if (mc.world.getBlockState(target).getBlock() == Blocks.ENDER_CHEST) {
            FindItemResult pick = InvUtils.findInHotbar(itemStack -> AutoTool.isEffectiveOn(itemStack.getItem(), Blocks.ENDER_CHEST.getDefaultState())
                && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) == 0);
            if (!pick.found()) return;

            InvUtils.swap(pick.getSlot());
            mc.interactionManager.updateBlockBreakingProgress(target, Direction.UP);
        }

        // Place echest if the target pos is empty
        if (mc.world.getBlockState(target).getMaterial().isReplaceable()) {
            FindItemResult echest = InvUtils.findInHotbar(Items.ENDER_CHEST);

            if (!echest.found()) {
                error("No Echests in hotbar, disabling");
                toggle();
                return;
            }

            BlockUtils.place(target, echest, true, 0, true);
        }
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (target == null || !render.get()) return;

        Box box = SHAPE.getBoundingBoxes().get(0);
        Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, target.getX() + box.minX, target.getY() + box.minY, target.getZ() + box.minZ, target.getX() + box.maxX, target.getY() + box.maxY, target.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
