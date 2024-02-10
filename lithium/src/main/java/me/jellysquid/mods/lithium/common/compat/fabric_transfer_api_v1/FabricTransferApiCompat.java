package me.jellysquid.mods.lithium.common.compat.fabric_transfer_api_v1;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FabricTransferApiCompat {
    public static final boolean FABRIC_TRANSFER_API_V_1_PRESENT;

    static {
        FABRIC_TRANSFER_API_V_1_PRESENT = FabricLoader.getInstance().isModLoaded("fabric-transfer-api-v1");
    }

    public static boolean canHopperInteractWithApiInventory(HopperBlockEntity hopperBlockEntity, BlockState hopperState, boolean extracting) {
        Direction direction = extracting ? Direction.UP : hopperState.get(HopperBlock.FACING);
        BlockPos targetPos = hopperBlockEntity.getPos().offset(direction);

        //noinspection UnstableApiUsage
        Object target = ItemStorage.SIDED.find(hopperBlockEntity.getWorld(), targetPos, direction.getOpposite());
        return target != null;
    }
}
