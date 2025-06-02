/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class AutoCity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgSupport = settings.createGroup("Support");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The radius in which players get targeted.")
        .defaultValue(5.5)
        .min(0)
        .sliderMax(7)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.LowestDistance)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches to your pickaxe automatically.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switches to your previous slot after mining.")
        .defaultValue(true)
        .visible(autoSwitch::get)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates server-side towards the block being placed/broken.")
        .defaultValue(true)
        .build()
    );

    // Break

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("Range in which to break blocks.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> breakWallsRange = sgBreak.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Range in which to break when behind blocks.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> packetMine = sgBreak.add(new BoolSetting.Builder()
        .name("packet-mine")
        .description("Sends packets to the block that is being broken.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> safeBreak = sgBreak.add(new BoolSetting.Builder()
        .name("safe-break")
        .description("Prevents breaking blocks that protect you from explosions.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SortMode> sortMode = sgBreak.add(new EnumSetting.Builder<SortMode>()
        .name("sort-mode")
        .description("The blocks you want to break first.")
        .defaultValue(SortMode.Furthest)
        .build()
    );

    // Support

    private final Setting<Boolean> support = sgSupport.add(new BoolSetting.Builder()
        .name("support")
        .description("Places obsidian under blocks that have been mined.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> placeRange = sgSupport.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The range at which the support block can be placed.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .visible(support::get)
        .build()
    );

    private final Setting<Double> placeWallsRange = sgSupport.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("Range in which to place the support block when behind blocks.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .visible(support::get)
        .build()
    );

    // Pause

    private final Setting<Boolean> pauseOnUse = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("Does not break blocks while using an item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseOnCA = sgPause.add(new BoolSetting.Builder()
        .name("pause-on-CA")
        .description("Does not break blocks while Crystal Aura is placing.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
        .name("swing")
        .description("Whether to swing hand client-side.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders the block that Auto City is breaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the rendering.")
        .defaultValue(new SettingColor(225, 0, 0, 65))
        .visible(() -> render.get() && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the rendering.")
        .defaultValue(new SettingColor(225, 0, 0, 255))
        .visible(() -> render.get() && shapeMode.get().lines())
        .build()
    );

    private PlayerEntity target;
    private BlockPos breakPos;
    private double progress;
    private Block lastBlock;
    private boolean mining;

    public AutoCity() {
        super(Categories.Combat, "auto-city", "Automatically mine blocks next to someone's feet.");
    }

    @Override
    public void onDeactivate() {
        breakPos = null;
        progress = 0;
        mining = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        FindItemResult tool = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
        if (!tool.isHotbar()) {
            error("No pickaxe found... disabling.");
            toggle();
            return;
        }

        // Pause
        if (shouldPause()) return;

        // Find target
        if (TargetUtils.isBadTarget(target, targetRange.get())) {
            onDeactivate();
            target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
            if (TargetUtils.isBadTarget(target, targetRange.get())) return;
        }

        // Attempt to place a support block under any recently broken block, if applicable
        BlockPos oldBreakPos = breakPos;
        if (support.get() && oldBreakPos != null && mc.world.getBlockState(oldBreakPos).getBlock() != lastBlock){
            placeSupport(oldBreakPos.down());
        }

        breakPos = getBlockToMine();
        if (breakPos == null) return;

        // Reset break progress if block changed
        Block block = mc.world.getBlockState(breakPos).getBlock();
        if (breakPos != oldBreakPos || block != lastBlock) {
            lastBlock = block;
            progress = 0;
            mining = false;
        }

        breakBlock(tool);
    }

    private void breakBlock(FindItemResult tool) {
        boolean hasPickaxe = autoSwitch.get() || mc.player.getInventory().getSelectedSlot() == tool.slot();

        // Packet mining mode
        if (packetMine.get()) {
            boolean start = progress == 0 && !mining;

            // Packets are only sent twice- at the beginning and end of the mining process
            if ((start || progress >= 1) && hasPickaxe) {
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(breakPos), Rotations.getPitch(breakPos), () -> packetMineBlock(tool, start));
                else packetMineBlock(tool, start);
            }

            if (mining && progress < 1) {
                progress += BlockUtils.getBreakDelta(tool.slot(), mc.world.getBlockState(breakPos));
            }
        // Legit mining mode
        } else {
            if (hasPickaxe) {
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(breakPos), Rotations.getPitch(breakPos), () -> mineBlock(tool));
                else mineBlock(tool);
            }
        }
    }

    public void packetMineBlock(FindItemResult tool, boolean start) {
        if (autoSwitch.get()) InvUtils.swap(tool.slot(), swapBack.get());

        Direction direction = BlockUtils.getDirection(breakPos);
        if (start) {
            // Begin mining the block
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, breakPos, direction));
            mining = true;
        } else {
            // Finish mining the block
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakPos, direction));
            progress = 0;
            mining = false;
        }

        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (swapBack.get()) InvUtils.swapBack();
    }

    public void mineBlock(FindItemResult tool) {
        if (autoSwitch.get()) InvUtils.swap(tool.slot(), swapBack.get());

        BlockUtils.breakBlock(breakPos, swing.get());

        if (swapBack.get()) InvUtils.swapBack();
    }

    private BlockPos getBlockToMine() {
        if (breakPos != null && canMineBlock(breakPos)) return breakPos;

        // Burrow block is first priority to break
        BlockPos targetPos = target.getBlockPos();
        if (canMineBlock(targetPos)) return targetPos;

        // Otherwise, break their surround blocks
        List<BlockPos> blocks = new ArrayList<>();
        for (Direction direction : Direction.HORIZONTAL) {
            BlockPos neighborPos = targetPos.offset(direction);
            if (canMineBlock(neighborPos) && !isRiskyBreak(neighborPos)) {
                blocks.add(neighborPos);
            }
        }

        if (blocks.isEmpty()) return null;

        // Sort blocks
        if (sortMode.get() != SortMode.None) {
            double pX = mc.player.getX();
            double pY = mc.player.getY();
            double pZ = mc.player.getZ();
            blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.Closest ? -1 : 1)));
        }

        return blocks.getLast();
    }

    private boolean canMineBlock(BlockPos blockPos) {
        // Block must be explosive resistant but breakable
        Block block = mc.world.getBlockState(blockPos).getBlock();
        if (block.getBlastResistance() < 600 || block.getHardness() < 0) return false;

        // Check range and raycast
        if (isOutOfRange(blockPos, breakRange.get(), breakWallsRange.get())) return false;

        return true;
    }

    private boolean isRiskyBreak(BlockPos blockPos) {
        if (!safeBreak.get()) return false;

        BlockPos myBlockPos = mc.player.getBlockPos();

        // If we are burrowed, the only risky break is our own burrow block
        Block block = mc.world.getBlockState(myBlockPos).getBlock();
        if (block.getBlastResistance() >= 600) return myBlockPos.equals(blockPos);

        // Otherwise, make certain we arent breaking our own surround blocks
        for (Direction direction : Direction.HORIZONTAL) {
            BlockPos neighborPos = myBlockPos.offset(direction);
            if (neighborPos.equals(blockPos)) return true;
        }

        return false;
    }

    private boolean isOutOfRange(BlockPos blockPos, double baseRange, double wallsRange) {
        Vec3d pos = blockPos.toCenterPos();
        if (!PlayerUtils.isWithin(pos, baseRange)) return true;

        RaycastContext raycastContext = new RaycastContext(mc.player.getEyePos(), pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(raycastContext);
        if (result == null || !result.getBlockPos().equals(blockPos))
            return !PlayerUtils.isWithin(pos, wallsRange);

        return false;
    }

    private void placeSupport(BlockPos blockPos) {
        FindItemResult item = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!item.found()) return;

        if (!BlockUtils.canPlace(blockPos)) return;

        // Check range and raycast
        if (isOutOfRange(blockPos, placeRange.get(), placeWallsRange.get())) return;

        BlockUtils.place(blockPos, item, rotate.get(), 40, swing.get());
    }

    private boolean shouldPause() {
        if (pauseOnUse.get() && mc.player.isUsingItem()) return true;

        CrystalAura CA = Modules.get().get(CrystalAura.class);
        if (pauseOnCA.get() && CA.isActive() && CA.kaTimer > 0) return true;

        return false;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get() || breakPos == null) return;
        event.renderer.box(breakPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    public enum SortMode {
        None,
        Closest,
        Furthest
    }
}
