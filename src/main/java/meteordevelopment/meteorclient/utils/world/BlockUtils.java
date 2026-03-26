/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.InstantRebreak;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@SuppressWarnings("ConstantConditions")
public class BlockUtils {
    public static boolean breaking;
    private static boolean breakingThisTick;

    private BlockUtils() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(BlockUtils.class);
    }

    // Placing

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, int rotationPriority) {
        return place(blockPos, findItemResult, rotationPriority, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority) {
        return place(blockPos, findItemResult, rotate, rotationPriority, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean checkEntities) {
        return place(blockPos, findItemResult, rotate, rotationPriority, true, checkEntities);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, int rotationPriority, boolean checkEntities) {
        return place(blockPos, findItemResult, true, rotationPriority, true, checkEntities);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities) {
        return place(blockPos, findItemResult, rotate, rotationPriority, swingHand, checkEntities, true);
    }

    public static boolean place(BlockPos blockPos, FindItemResult findItemResult, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (findItemResult.isOffhand()) {
            return place(blockPos, InteractionHand.OFF_HAND, mc.player.getInventory().getSelectedSlot(), rotate, rotationPriority, swingHand, checkEntities, swapBack);
        } else if (findItemResult.isHotbar()) {
            return place(blockPos, InteractionHand.MAIN_HAND, findItemResult.slot(), rotate, rotationPriority, swingHand, checkEntities, swapBack);
        }
        return false;
    }

    public static boolean place(BlockPos blockPos, InteractionHand hand, int slot, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (slot < 0 || slot > 8) return false;

        Block toPlace = Blocks.OBSIDIAN;
        ItemStack i = hand == InteractionHand.MAIN_HAND ? mc.player.getInventory().getItem(slot) : mc.player.getInventory().getItem(SlotUtils.OFFHAND);
        if (i.getItem() instanceof BlockItem blockItem) toPlace = blockItem.getBlock();
        if (!canPlaceBlock(blockPos, checkEntities, toPlace)) return false;

        Vec3 hitPos = Vec3.atCenterOf(blockPos);

        BlockPos neighbour;
        Direction side = getPlaceSide(blockPos);

        if (side == null) {
            side = Direction.UP;
            neighbour = blockPos;
        } else {
            neighbour = blockPos.relative(side);
            hitPos = hitPos.add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
        }

        BlockHitResult bhr = new BlockHitResult(hitPos, side.getOpposite(), neighbour, false);

        if (rotate) {
            Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), rotationPriority, () -> {
                InvUtils.swap(slot, swapBack);

                interact(bhr, hand, swingHand);

                if (swapBack) InvUtils.swapBack();
            });
        } else {
            InvUtils.swap(slot, swapBack);

            interact(bhr, hand, swingHand);

            if (swapBack) InvUtils.swapBack();
        }


        return true;
    }

    public static void interact(BlockHitResult blockHitResult, InteractionHand hand, boolean swing) {
        boolean wasSneaking = mc.player.isShiftKeyDown();
        mc.player.setShiftKeyDown(false);

        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, blockHitResult);

        if (result.consumesAction()) {
            if (swing) mc.player.swing(hand);
            else mc.getConnection().getConnection().send(new ServerboundSwingPacket(hand));
        }

        mc.player.setShiftKeyDown(wasSneaking);
    }

    public static boolean canPlaceBlock(BlockPos blockPos, boolean checkEntities, Block block) {
        if (blockPos == null) return false;

        // Check y level
        if (!Level.isInSpawnableBounds(blockPos)) return false;

        // Check if current block is replaceable
        if (!mc.level.getBlockState(blockPos).canBeReplaced()) return false;

        // Check if intersects entities
        return !checkEntities || mc.level.isUnobstructed(block.defaultBlockState(), blockPos, CollisionContext.empty());
    }

    public static boolean canPlace(BlockPos blockPos, boolean checkEntities) {
        return canPlaceBlock(blockPos, checkEntities, Blocks.OBSIDIAN);
    }

    public static boolean canPlace(BlockPos blockPos) {
        return canPlace(blockPos, true);
    }

    public static Direction getPlaceSide(BlockPos blockPos) {
        Vec3 lookVec = blockPos.getCenter().subtract(mc.player.getEyePosition());
        double bestRelevancy = -Double.MAX_VALUE;
        Direction bestSide = null;

        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.relative(side);
            BlockState state = mc.level.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            double relevancy = side.getAxis().choose(lookVec.x(), lookVec.y(), lookVec.z()) * side.getAxisDirection().getStep();
            if (relevancy > bestRelevancy) {
                bestRelevancy = relevancy;
                bestSide = side;
            }
        }

        return bestSide;
    }

    public static Direction getClosestPlaceSide(BlockPos blockPos) {
        return getClosestPlaceSide(blockPos, mc.player.getEyePosition());
    }

    public static Direction getClosestPlaceSide(BlockPos blockPos, Vec3 pos) {
        Direction closestSide = null;
        double closestDistance = Double.MAX_VALUE;

        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.relative(side);
            BlockState state = mc.level.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            double distance = pos.distanceToSqr(neighbor.getX(), neighbor.getY(), neighbor.getZ());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestSide = side;
            }
        }

        return closestSide;
    }

    // Breaking

    @EventHandler(priority = EventPriority.HIGHEST + 100)
    private static void onTickPre(TickEvent.Pre event) {
        breakingThisTick = false;
    }

    @EventHandler(priority = EventPriority.LOWEST - 100)
    private static void onTickPost(TickEvent.Post event) {
        if (!breakingThisTick && breaking) {
            breaking = false;
            if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        }
    }

    /**
     * Needs to be used in {@link TickEvent.Pre}
     */
    public static boolean breakBlock(BlockPos blockPos, boolean swing) {
        if (!canBreak(blockPos, mc.level.getBlockState(blockPos))) return false;

        // Creating new instance of block pos because minecraft assigns the parameter to a field, and we don't want it to change when it has been stored in a field somewhere
        BlockPos pos = blockPos instanceof BlockPos.MutableBlockPos ? new BlockPos(blockPos) : blockPos;

        InstantRebreak ir = Modules.get().get(InstantRebreak.class);
        if (ir != null && ir.isActive() && ir.blockPos.equals(pos) && ir.shouldMine()) {
            ir.sendPacket();
            return true;
        }

        if (mc.gameMode.isDestroying())
            mc.gameMode.continueDestroyBlock(pos, getDirection(blockPos));
        else mc.gameMode.startDestroyBlock(pos, getDirection(blockPos));

        if (swing) mc.player.swing(InteractionHand.MAIN_HAND);
        else mc.getConnection().getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

        breaking = true;
        breakingThisTick = true;

        return true;
    }

    public static boolean canBreak(BlockPos blockPos, BlockState state) {
        if (!mc.player.isCreative() && state.getDestroySpeed(mc.level, blockPos) < 0) return false;
        return state.getShape(mc.level, blockPos) != Shapes.empty();
    }

    public static boolean canBreak(BlockPos blockPos) {
        return canBreak(blockPos, mc.level.getBlockState(blockPos));
    }

    public static boolean canInstaBreak(BlockPos blockPos, float breakSpeed) {
        return mc.player.isCreative() || calcBlockBreakingDelta2(blockPos, breakSpeed) >= 1;
    }

    public static boolean canInstaBreak(BlockPos blockPos) {
        BlockState state = mc.level.getBlockState(blockPos);
        return canInstaBreak(blockPos, mc.player.getDestroySpeed(state));
    }

    public static float calcBlockBreakingDelta2(BlockPos blockPos, float breakSpeed) {
        BlockState state = mc.level.getBlockState(blockPos);
        float f = state.getDestroySpeed(mc.level, blockPos);
        if (f == -1.0F) {
            return 0.0F;
        } else {
            int i = mc.player.hasCorrectToolForDrops(state) ? 30 : 100;
            return breakSpeed / f / (float) i;
        }
    }

    // Other

    public static boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock
            || block instanceof AnvilBlock
            || block instanceof LoomBlock
            || block instanceof CartographyTableBlock
            || block instanceof GrindstoneBlock
            || block instanceof StonecutterBlock
            || block instanceof ButtonBlock
            || block instanceof BasePressurePlateBlock
            || block instanceof BaseEntityBlock
            || block instanceof BedBlock
            || block instanceof FenceGateBlock
            || block instanceof DoorBlock
            || block instanceof NoteBlock
            || block instanceof TrapDoorBlock;
    }


    public static MobSpawn isValidMobSpawn(BlockPos blockPos, BlockState blockState, int spawnLightLimit) {
        boolean snow = blockState.getBlock() instanceof SnowLayerBlock && blockState.getValue(SnowLayerBlock.LAYERS) == 1;
        if (!blockState.isAir() && !snow) return MobSpawn.Never;

        if (!isValidSpawnBlock(mc.level.getBlockState(blockPos.below()))) return MobSpawn.Never;

        if (mc.level.getBrightness(LightLayer.BLOCK, blockPos) > spawnLightLimit) return MobSpawn.Never;
        else if (mc.level.getBrightness(LightLayer.SKY, blockPos) > spawnLightLimit) return  MobSpawn.Potential;

        return MobSpawn.Always;
    }

    public static boolean isValidSpawnBlock(BlockState blockState) {
        Block block = blockState.getBlock();

        if (block == Blocks.BEDROCK
            || block == Blocks.BARRIER
            || block instanceof TransparentBlock
            || block instanceof ScaffoldingBlock) return false;

        if (block == Blocks.SOUL_SAND || block == Blocks.MUD) return true;
        if (block instanceof SlabBlock && blockState.getValue(SlabBlock.TYPE) == SlabType.TOP) return true;
        if (block instanceof StairBlock && blockState.getValue(StairBlock.HALF) == Half.TOP) return true;

        return blockState.isSolidRender();
    }

    // Finds the best block direction to get when interacting with the block.
    public static Direction getDirection(BlockPos pos) {
        double eyePos = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        VoxelShape outline = mc.level.getBlockState(pos).getCollisionShape(mc.level, pos);

        if (eyePos > pos.getY() + outline.max(Direction.Axis.Y) && mc.level.getBlockState(pos.above()).canBeReplaced()) {
            return Direction.UP;
        } else if (eyePos < pos.getY() + outline.min(Direction.Axis.Y) && mc.level.getBlockState(pos.below()).canBeReplaced()) {
            return Direction.DOWN;
        } else {
            BlockPos difference = pos.subtract(mc.player.blockPosition());

            if (Math.abs(difference.getX()) > Math.abs(difference.getZ())) {
                return difference.getX() > 0 ? Direction.WEST : Direction.EAST;
            } else {
                return difference.getZ() > 0 ? Direction.NORTH : Direction.SOUTH;
            }
        }
    }

    public enum MobSpawn {
        Never,
        Potential,
        Always
    }

    private static final ThreadLocal<BlockPos.MutableBlockPos> EXPOSED_POS = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public static boolean isExposed(BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            if (!mc.level.getBlockState(EXPOSED_POS.get().setWithOffset(blockPos, direction)).isSolidRender()) return true;
        }

        return false;
    }

    public static double getBreakDelta(int slot, BlockState state) {
        float hardness = state.getDestroySpeed(null, null);
        if (hardness == -1) return 0;
        else {
            return getBlockBreakingSpeed(slot, state) / hardness / (!state.requiresCorrectToolForDrops() || mc.player.getInventory().getNonEquipmentItems().get(slot).isCorrectToolForDrops(state) ? 30 : 100);
        }
    }

    /**
     * @see net.minecraft.world.entity.player.Player#getDestroySpeed(BlockState)
     */
    private static double getBlockBreakingSpeed(int slot, BlockState block) {
        double speed = mc.player.getInventory().getNonEquipmentItems().get(slot).getDestroySpeed(block);

        if (speed > 1) {
            ItemStack tool = mc.player.getInventory().getItem(slot);

            int efficiency = Utils.getEnchantmentLevel(tool, Enchantments.EFFICIENCY);

            if (efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        }

        if (MobEffectUtil.hasDigSpeed(mc.player)) {
            speed *= 1 + (MobEffectUtil.getDigSpeedAmplification(mc.player) + 1) * 0.2F;
        }

        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            float k = switch (mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            speed *= k;
        }

        if (mc.player.isEyeInFluid(FluidTags.WATER)) {
            speed *= mc.player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
        }

        if (!mc.player.onGround()) {
            speed /= 5.0F;
        }

        return speed;
    }

    /**
     * Mutates a {@link BlockPos.MutableBlockPos} around an origin
     */
    public static BlockPos.MutableBlockPos mutateAround(BlockPos.MutableBlockPos mutable, BlockPos origin, int xOffset, int yOffset, int zOffset) {
        return mutable.set(origin.getX() + xOffset, origin.getY() + yOffset, origin.getZ() + zOffset);
    }
}
