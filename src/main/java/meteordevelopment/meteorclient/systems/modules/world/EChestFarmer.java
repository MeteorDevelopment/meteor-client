/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
        .range(8, 512)
        .sliderRange(8, 128)
        .visible(selfToggle::get)
        .build()
    );

    // Render

    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client-side.")
        .defaultValue(true)
        .build()
    );

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
    private int startCount;

    public EChestFarmer() {
        super(Categories.World, "echest-farmer", "Places and breaks EChests to farm obsidian.");
    }

    @Override
    public void onActivate() {
        target = null;
        startCount = InvUtils.find(Items.OBSIDIAN).count();
    }

    @Override
    public void onDeactivate() {
        InvUtils.swapBack();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Finding target pos
        if (target == null) {
            if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            BlockState state = mc.world.getBlockState(pos);

            if (state.getMaterial().isReplaceable() || state.getBlock() == Blocks.ENDER_CHEST) {
                target = ((BlockHitResult) mc.crosshairTarget).getBlockPos().up();
            }
            else return;
        }

        // Disable if the block is too far away
        if (!PlayerUtils.isWithinReach(target)) {
            error("Target block pos out of reach.");
            target = null;
            return;
        }

        // Toggle if obby amount reached
        if (selfToggle.get() && InvUtils.find(Items.OBSIDIAN).count() - (ignoreExisting.get() ? startCount : 0) >= amount.get()) {
            InvUtils.swapBack();
            toggle();
            return;
        }

        // Break existing echest at target pos
        if (mc.world.getBlockState(target).getBlock() == Blocks.ENDER_CHEST) {
            double bestScore = -1;
            int bestSlot = -1;

            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = mc.player.getInventory().getStack(i);
                if (EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) > 0) continue;

                double score = itemStack.getMiningSpeedMultiplier(Blocks.ENDER_CHEST.getDefaultState());

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }

            if (bestSlot == -1) return;

            InvUtils.swap(bestSlot, true);
            BlockUtils.breakBlock(target, swingHand.get());
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
    private void onRender(Render3DEvent event) {
        if (target == null || !render.get() || Modules.get().get(PacketMine.class).isMiningBlock(target)) return;

        Box box = SHAPE.getBoundingBoxes().get(0);
        event.renderer.box(target.getX() + box.minX, target.getY() + box.minY, target.getZ() + box.minZ, target.getX() + box.maxX, target.getY() + box.maxY, target.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
