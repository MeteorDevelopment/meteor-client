/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import com.google.common.collect.ImmutableMap;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.MissingItemModel;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;

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
            return place(blockPos, Hand.OFF_HAND, mc.player.getInventory().getSelectedSlot(), rotate, rotationPriority, swingHand, checkEntities, swapBack);
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
        boolean wasSneaking = mc.player.isSneaking();
        mc.player.setSneaking(false);

        ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);

        if (result.isAccepted()) {
            if (swing) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }

        mc.player.setSneaking(wasSneaking);
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

    // GUI Display

    // special blocks with no standard block models, manually map to item model
    private static final Map<Block, Item> HARDCODED_MAPPINGS = Map.of(
        Blocks.WATER, Items.WATER_BUCKET,
        Blocks.LAVA, Items.LAVA_BUCKET,
        Blocks.BUBBLE_COLUMN, Items.WATER_BUCKET,
        Blocks.END_PORTAL, Items.ENDER_EYE,
        Blocks.END_GATEWAY, Items.ENDER_EYE,
        Blocks.PISTON_HEAD, Items.PISTON,
        Blocks.MOVING_PISTON, Items.PISTON
    );

    // block whose block models look better than their item models
    private static final Set<Block> FORCED_BLOCK_DISPLAY = Set.of(
        Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON
    );

    public static ItemStack getDisplayStack(Block block) {
        ItemStack stack = block.asItem().getDefaultStack();
        if (stack.isEmpty() == block.getDefaultState().isAir() && !FORCED_BLOCK_DISPLAY.contains(block)) {
            return stack;
        }

        if (HARDCODED_MAPPINGS.containsKey(block)) {
            return HARDCODED_MAPPINGS.get(block).getDefaultStack();
        }

        // replace with block model
        Identifier blockId = Registries.BLOCK.getId(block);
        Identifier displayModelId = MeteorClient.identifier(blockId.getPath() + "_display");

        ItemModel model = MinecraftClient.getInstance().getBakedModelManager().getItemModel(displayModelId);

        if (!(model instanceof MissingItemModel)) {
            ItemStack replacement = Items.STICK.getDefaultStack(); // cant be air
            replacement.set(DataComponentTypes.ITEM_MODEL, displayModelId);
            return replacement;
        }

        // unknown missing block, render nothing
        return stack;
    }

    private static final Map<Block, String> BLOCK_NAME_OVERRIDES = ImmutableMap.<Block, String>builder()
        .put(Blocks.WALL_TORCH, "Wall Torch")
        .put(Blocks.REDSTONE_WALL_TORCH, "Redstone Wall Torch")
        .put(Blocks.SOUL_WALL_TORCH, "Soul Wall Torch")
        .put(Blocks.COPPER_WALL_TORCH, "Copper Wall Torch")

        .put(Blocks.SKELETON_WALL_SKULL, "Skeleton Wall Skull")
        .put(Blocks.WITHER_SKELETON_WALL_SKULL, "Wither Skeleton Wall Skull")
        .put(Blocks.ZOMBIE_WALL_HEAD, "Zombie Wall Head")
        .put(Blocks.PLAYER_WALL_HEAD, "Player Wall Head")
        .put(Blocks.DRAGON_WALL_HEAD, "Dragon Wall Head")
        .put(Blocks.PIGLIN_WALL_HEAD, "Piglin Wall Head")
        .put(Blocks.CREEPER_WALL_HEAD, "Creeper Wall Head")

        .put(Blocks.OAK_WALL_SIGN, "Oak Wall Sign")
        .put(Blocks.BIRCH_WALL_SIGN, "Birch Wall Sign")
        .put(Blocks.SPRUCE_WALL_SIGN, "Spruce Wall Sign")
        .put(Blocks.ACACIA_WALL_SIGN, "Acacia Wall Sign")
        .put(Blocks.CHERRY_WALL_SIGN, "Cherry Wall Sign")
        .put(Blocks.JUNGLE_WALL_SIGN, "Jungle Wall Sign")
        .put(Blocks.BAMBOO_WALL_SIGN, "Bamboo Wall Sign")
        .put(Blocks.WARPED_WALL_SIGN, "Warped Wall Sign")
        .put(Blocks.CRIMSON_WALL_SIGN, "Crimson Wall Sign")
        .put(Blocks.DARK_OAK_WALL_SIGN, "Dark Oak Wall Sign")
        .put(Blocks.PALE_OAK_WALL_SIGN, "Pale Oak Wall Sign")
        .put(Blocks.MANGROVE_WALL_SIGN, "Mangrove Wall Sign")

        .put(Blocks.OAK_WALL_HANGING_SIGN, "Oak Wall Hanging Sign")
        .put(Blocks.BIRCH_WALL_HANGING_SIGN, "Birch Wall Hanging Sign")
        .put(Blocks.SPRUCE_WALL_HANGING_SIGN, "Spruce Wall Hanging Sign")
        .put(Blocks.ACACIA_WALL_HANGING_SIGN, "Acacia Wall Hanging Sign")
        .put(Blocks.CHERRY_WALL_HANGING_SIGN, "Cherry Wall Hanging Sign")
        .put(Blocks.JUNGLE_WALL_HANGING_SIGN, "Jungle Wall Hanging Sign")
        .put(Blocks.BAMBOO_WALL_HANGING_SIGN, "Bamboo Wall Hanging Sign")
        .put(Blocks.WARPED_WALL_HANGING_SIGN, "Warped Wall Hanging Sign")
        .put(Blocks.CRIMSON_WALL_HANGING_SIGN, "Crimson Wall Hanging Sign")
        .put(Blocks.DARK_OAK_WALL_HANGING_SIGN, "Dark Oak Wall Hanging Sign")
        .put(Blocks.PALE_OAK_WALL_HANGING_SIGN, "Pale Oak Wall Hanging Sign")
        .put(Blocks.MANGROVE_WALL_HANGING_SIGN, "Mangrove Wall Hanging Sign")
        .buildOrThrow();

    public static String getDisplayName(Block block) {
        if (BLOCK_NAME_OVERRIDES.containsKey(block)) {
            return BLOCK_NAME_OVERRIDES.get(block);
        } else {
            return StringHelper.stripTextFormat(I18n.translate(block.getTranslationKey()));
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


    public static MobSpawn isValidMobSpawn(BlockPos blockPos, BlockState blockState, int spawnLightLimit) {
        boolean snow = blockState.getBlock() instanceof SnowBlock && blockState.get(SnowBlock.LAYERS) == 1;
        if (!blockState.isAir() && !snow) return MobSpawn.Never;

        if (!isValidSpawnBlock(mc.world.getBlockState(blockPos.down()))) return MobSpawn.Never;

        if (mc.world.getLightLevel(LightType.BLOCK, blockPos) > spawnLightLimit) return MobSpawn.Never;
        else if (mc.world.getLightLevel(LightType.SKY, blockPos) > spawnLightLimit) return  MobSpawn.Potential;

        return MobSpawn.Always;
    }

    public static boolean isValidSpawnBlock(BlockState blockState) {
        Block block = blockState.getBlock();

        if (block == Blocks.BEDROCK
            || block == Blocks.BARRIER
            || block instanceof TransparentBlock
            || block instanceof ScaffoldingBlock) return false;

        if (block == Blocks.SOUL_SAND || block == Blocks.MUD) return true;
        if (block instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.TOP) return true;
        if (block instanceof StairsBlock && blockState.get(StairsBlock.HALF) == BlockHalf.TOP) return true;

        return blockState.isOpaqueFullCube();
    }

    // Finds the best block direction to get when interacting with the block.
    public static Direction getDirection(BlockPos pos) {
        double eyePos = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        VoxelShape outline = mc.world.getBlockState(pos).getCollisionShape(mc.world, pos);

        if (eyePos > pos.getY() + outline.getMax(Direction.Axis.Y) && mc.world.getBlockState(pos.up()).isReplaceable()) {
            return Direction.UP;
        } else if (eyePos < pos.getY() + outline.getMin(Direction.Axis.Y) && mc.world.getBlockState(pos.down()).isReplaceable()) {
            return Direction.DOWN;
        } else {
            BlockPos difference = pos.subtract(mc.player.getBlockPos());

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

    private static final ThreadLocal<BlockPos.Mutable> EXPOSED_POS = ThreadLocal.withInitial(BlockPos.Mutable::new);

    public static boolean isExposed(BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            if (!mc.world.getBlockState(EXPOSED_POS.get().set(blockPos, direction)).isOpaqueFullCube()) return true;
        }

        return false;
    }

    public static double getBreakDelta(int slot, BlockState state) {
        float hardness = state.getHardness(null, null);
        if (hardness == -1) return 0;
        else {
            return getBlockBreakingSpeed(slot, state) / hardness / (!state.isToolRequired() || mc.player.getInventory().getMainStacks().get(slot).isSuitableFor(state) ? 30 : 100);
        }
    }

    /**
     * @see net.minecraft.entity.player.PlayerEntity#getBlockBreakingSpeed(BlockState)
     */
    private static double getBlockBreakingSpeed(int slot, BlockState block) {
        double speed = mc.player.getInventory().getMainStacks().get(slot).getMiningSpeedMultiplier(block);

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

        if (mc.player.isSubmergedIn(FluidTags.WATER)) {
            speed *= mc.player.getAttributeValue(EntityAttributes.SUBMERGED_MINING_SPEED);
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
