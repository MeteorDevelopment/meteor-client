package minegame159.meteorclient.utils.world;

import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BlockUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Vec3d hitPos = new Vec3d(0, 0, 0);

    public static boolean place(BlockPos blockPos, Hand hand, int slot, boolean rotate, int priority, boolean swing) {
        if (slot == -1 || !canPlace(blockPos)) return false;

        Direction side = getPlaceSide(blockPos);
        BlockPos neighbour;
        Vec3d hitPos = rotate ? new Vec3d(0, 0, 0) : BlockUtils.hitPos;

        if (side == null) {
            side = Direction.UP;
            neighbour = blockPos;
            ((IVec3d) hitPos).set(blockPos);
        }
        else {
            neighbour = blockPos.offset(side.getOpposite());
            ((IVec3d) hitPos).set(neighbour.getX() + 0.5 + side.getOffsetX() * 0.5, neighbour.getY() + 0.5 + side.getOffsetY() * 0.5, neighbour.getZ() + 0.5 + side.getOffsetZ() * 0.5);
        }

        if (rotate) {
            Direction s = side;
            Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), priority, () -> place(slot, hitPos, hand, s, neighbour, swing));
        }
        else place(slot, hitPos, hand, side, neighbour, swing);

        return true;
    }
    public static boolean place(BlockPos blockPos, Hand hand, int slot, boolean rotate, int priority) {
        return place(blockPos, hand, slot, rotate, priority, true);
    }

    private static void place(int slot, Vec3d hitPos, Hand hand, Direction side, BlockPos neighbour, boolean swing) {
        int preSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = slot;

        boolean wasSneaking = mc.player.input.sneaking;
        mc.player.input.sneaking = false;

        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(hitPos, side, neighbour, false));
        if (swing) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        mc.player.input.sneaking = wasSneaking;

        mc.player.inventory.selectedSlot = preSlot;
    }

    public static boolean canPlace(BlockPos blockPos) {
        if (blockPos == null) return false;

        // Check y level
        if (World.isOutOfBuildLimitVertically(blockPos)) return false;

        // Check if current block is replaceable
        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        // Check if intersects entities
        return mc.world.canPlace(Blocks.STONE.getDefaultState(), blockPos, ShapeContext.absent());
    }

    public static boolean isClickable(Block block) {
        boolean clickable = false;

        if (block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof AbstractButtonBlock
                || block instanceof AbstractPressurePlateBlock
                || block instanceof BlockWithEntity
                || block instanceof FenceGateBlock
                || block instanceof DoorBlock
                || block instanceof TrapdoorBlock
        ) clickable = true;

        return clickable;
    }

    private static Direction getPlaceSide(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            // Check if neighbour isn't empty
            if (mc.world.getBlockState(neighbor).isAir() || isClickable(mc.world.getBlockState(neighbor).getBlock())) continue;

            return side2;
        }

        return null;
    }
}
