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
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

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
            return place(blockPos, Hand.OFF_HAND, mc.player.getInventory().selectedSlot, rotate, rotationPriority, swingHand, checkEntities, swapBack);
        } else if (findItemResult.isHotbar()) {
            return place(blockPos, Hand.MAIN_HAND, findItemResult.slot(), rotate, rotationPriority, swingHand, checkEntities, swapBack);
        }
        return false;
    }

    public static boolean place(BlockPos blockPos, Hand hand, int slot, boolean rotate, int rotationPriority, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (slot < 0 || slot > 8) return false;

        Block toPlace = Blocks.OBSIDIAN;
        ItemStack i = hand == Hand.MAIN_HAND ? mc.player.getInventory().getStack(slot) : mc.player.getInventory().getStack(SlotUtils.OFFHAND);
        if (i.getItem() instanceof BlockItem blockItem) toPlace = blockItem.getBlock();
        if (!canPlaceBlock(blockPos, checkEntities, toPlace)) return false;

        Vec3d hitPos = Vec3d.ofCenter(blockPos);

        BlockPos neighbour;
        Direction side = getPlaceSide(blockPos);

        if (side == null) {
            side = Direction.UP;
            neighbour = blockPos;
        } else {
            neighbour = blockPos.offset(side);
            hitPos = hitPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
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

    public static void interact(BlockHitResult blockHitResult, Hand hand, boolean swing) {
        boolean wasSneaking = mc.player.input.sneaking;
        mc.player.input.sneaking = false;

        ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);

        if (result.shouldSwingHand()) {
            if (swing) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }

        mc.player.input.sneaking = wasSneaking;
    }

    public static boolean canPlaceBlock(BlockPos blockPos, boolean checkEntities, Block block) {
        if (blockPos == null) return false;

        // Check y level
        if (!World.isValid(blockPos)) return false;

        // Check if current block is replaceable
        if (!mc.world.getBlockState(blockPos).isReplaceable()) return false;

        // Check if intersects entities
        return !checkEntities || mc.world.canPlace(block.getDefaultState(), blockPos, ShapeContext.absent());
    }

    public static boolean canPlace(BlockPos blockPos, boolean checkEntities) {
        return canPlaceBlock(blockPos, checkEntities, Blocks.OBSIDIAN);
    }

    public static boolean canPlace(BlockPos blockPos) {
        return canPlace(blockPos, true);
    }

    public static Direction getPlaceSide(BlockPos blockPos) {
        Vec3d lookVec = blockPos.toCenterPos().subtract(mc.player.getEyePos());
        double bestRelevancy = -Double.MAX_VALUE;
        Direction bestSide = null;

        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            BlockState state = mc.world.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            double relevancy = side.getAxis().choose(lookVec.getX(), lookVec.getY(), lookVec.getZ()) * side.getDirection().offset();
            if (relevancy > bestRelevancy) {
                bestRelevancy = relevancy;
                bestSide = side;
            }
        }

        return bestSide;
    }

    public static Direction getClosestPlaceSide(BlockPos blockPos) {
        return getClosestPlaceSide(blockPos, mc.player.getEyePos());
    }

    public static Direction getClosestPlaceSide(BlockPos blockPos, Vec3d pos) {
        Direction closestSide = null;
        double closestDistance = Double.MAX_VALUE;

        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            BlockState state = mc.world.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            double distance = pos.squaredDistanceTo(neighbor.getX(), neighbor.getY(), neighbor.getZ());
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
            if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
        }
    }

    /**
     * Needs to be used in {@link TickEvent.Pre}
     */
    public static boolean breakBlock(BlockPos blockPos, boolean swing) {
        if (!canBreak(blockPos, mc.world.getBlockState(blockPos))) return false;

        // Creating new instance of block pos because minecraft assigns the parameter to a field, and we don't want it to change when it has been stored in a field somewhere
        BlockPos pos = blockPos instanceof BlockPos.Mutable ? new BlockPos(blockPos) : blockPos;

        InstantRebreak ir = Modules.get().get(InstantRebreak.class);
        if (ir != null && ir.isActive() && ir.blockPos.equals(pos) && ir.shouldMine()) {
            ir.sendPacket();
            return true;
        }

        if (mc.interactionManager.isBreakingBlock())
            mc.interactionManager.updateBlockBreakingProgress(pos, getDirection(blockPos));
        else mc.interactionManager.attackBlock(pos, getDirection(blockPos));

        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        breaking = true;
        breakingThisTick = true;

        return true;
    }

    public static boolean canBreak(BlockPos blockPos, BlockState state) {
        if (!mc.player.isCreative() && state.getHardness(mc.world, blockPos) < 0) return false;
        return state.getOutlineShape(mc.world, blockPos) != VoxelShapes.empty();
    }

    public static boolean canBreak(BlockPos blockPos) {
        return canBreak(blockPos, mc.world.getBlockState(blockPos));
    }

    public static boolean canInstaBreak(BlockPos blockPos, float breakSpeed) {
        return mc.player.isCreative() || calcBlockBreakingDelta2(blockPos, breakSpeed) >= 1;
    }

    public static boolean canInstaBreak(BlockPos blockPos) {
        BlockState state = mc.world.getBlockState(blockPos);
        return canInstaBreak(blockPos, mc.player.getBlockBreakingSpeed(state));
    }

    public static float calcBlockBreakingDelta2(BlockPos blockPos, float breakSpeed) {
        BlockState state = mc.world.getBlockState(blockPos);
        float f = state.getHardness(mc.world, blockPos);
        if (f == -1.0F) {
            return 0.0F;
        } else {
            int i = mc.player.canHarvest(state) ? 30 : 100;
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
            || block instanceof AbstractPressurePlateBlock
            || block instanceof BlockWithEntity
            || block instanceof BedBlock
            || block instanceof FenceGateBlock
            || block instanceof DoorBlock
            || block instanceof NoteBlock
            || block instanceof TrapdoorBlock;
    }

    public static MobSpawn isValidMobSpawn(BlockPos blockPos, boolean newMobSpawnLightLevel) {
        return isValidMobSpawn(blockPos, mc.world.getBlockState(blockPos), newMobSpawnLightLevel ? 0 : 7);
    }

    public static MobSpawn isValidMobSpawn(BlockPos blockPos, BlockState blockState, int spawnLightLimit) {
        if (!(blockState.getBlock() instanceof AirBlock)) return MobSpawn.Never;

        BlockPos down = blockPos.down();
        BlockState downState = mc.world.getBlockState(down);
        if (downState.getBlock() == Blocks.BEDROCK) return MobSpawn.Never;

        if (!topSurface(downState)) {
            if (downState.getCollisionShape(mc.world, down) != VoxelShapes.fullCube())
                return MobSpawn.Never;
            if (downState.isTransparent(mc.world, down)) return MobSpawn.Never;
        }

        if (mc.world.getLightLevel(LightType.BLOCK, blockPos) > spawnLightLimit) return MobSpawn.Never;
        else if (mc.world.getLightLevel(LightType.SKY, blockPos) > spawnLightLimit) return  MobSpawn.Potential;

        return MobSpawn.Always;
    }

    public static boolean topSurface(BlockState blockState) {
        if (blockState.getBlock() instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.TOP) return true;
        else return blockState.getBlock() instanceof StairsBlock && blockState.get(StairsBlock.HALF) == BlockHalf.TOP;
    }

    // Finds the best block direction to get when interacting with the block.
    public static Direction getDirection(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        if ((double) pos.getY() > eyesPos.y) {
            if (mc.world.getBlockState(pos.add(0, -1, 0)).isReplaceable()) return Direction.DOWN;
            else return mc.player.getHorizontalFacing().getOpposite();
        }
        if (!mc.world.getBlockState(pos.add(0, 1, 0)).isReplaceable()) return mc.player.getHorizontalFacing().getOpposite();
        return Direction.UP;
    }

    public enum MobSpawn {
        Never,
        Potential,
        Always
    }

    private static final ThreadLocal<BlockPos.Mutable> EXPOSED_POS = ThreadLocal.withInitial(BlockPos.Mutable::new);

    public static boolean isExposed(BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            if (!mc.world.getBlockState(EXPOSED_POS.get().set(blockPos, direction)).isOpaque()) return true;
        }

        return false;
    }

    public static double getBreakDelta(int slot, BlockState state) {
        float hardness = state.getHardness(null, null);
        if (hardness == -1) return 0;
        else {
            return getBlockBreakingSpeed(slot, state) / hardness / (!state.isToolRequired() || mc.player.getInventory().main.get(slot).isSuitableFor(state) ? 30 : 100);
        }
    }

    private static double getBlockBreakingSpeed(int slot, BlockState block) {
        double speed = mc.player.getInventory().main.get(slot).getMiningSpeedMultiplier(block);

        if (speed > 1) {
            ItemStack tool = mc.player.getInventory().getStack(slot);

            int efficiency = Utils.getEnchantmentLevel(tool, Enchantments.EFFICIENCY);

            if (efficiency > 0 && !tool.isEmpty()) speed += efficiency * efficiency + 1;
        }

        if (StatusEffectUtil.hasHaste(mc.player)) {
            speed *= 1 + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
        }

        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float k = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            speed *= k;
        }

        if (mc.player.isSubmergedIn(FluidTags.WATER) /*fixme && !EnchantmentHelper.hasAquaAffinity(mc.player)*/) {
            speed /= 5.0F;
        }

        if (!mc.player.isOnGround()) {
            speed /= 5.0F;
        }

        return speed;
    }

    /**
     * Mutates a {@link BlockPos.Mutable} around an origin
     */
    public static BlockPos.Mutable mutateAround(BlockPos.Mutable mutable, BlockPos origin, int xOffset, int yOffset, int zOffset) {
        return mutable.set(origin.getX() + xOffset, origin.getY() + yOffset, origin.getZ() + zOffset);
    }
}
