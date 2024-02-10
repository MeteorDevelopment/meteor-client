package me.jellysquid.mods.lithium.mixin.block.redstone_wire;

import me.jellysquid.mods.lithium.common.util.DirectionConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Optimizing redstone dust is tricky, but even more so if you wish to preserve behavior
 * perfectly. There are two big reasons redstone wire is laggy:
 * <br>
 * - It updates recursively. Each wire updates its power level in isolation, rather than
 *   in the context of the network it is a part of. This means each wire in a network
 *   could check and update its power level over half a dozen times. This also leads to
 *   way more shape and block updates than necessary.
 * <br>
 * - It emits copious amounts of duplicate and redundant shape and block updates. While
 *   the recursive updates are largely to blame, even a single state change leads to 18
 *   redundant block updates and up to 16 redundant shape updates.
 * 
 * <p>
 * Unfortunately fixing either of these aspects can be detected in-game, even if it is
 * through obscure mechanics. Removing redundant block updates can be detected with
 * something as simple as a redstone wire on a trapdoor, while removing the recursive
 * updates can be detected with locational setups that rely on a specific block update
 * order.
 * 
 * <p>
 * What we can optimize, however, are the power calculations. In vanilla, these are split
 * into two parts:
 * <br>
 * - Power from non-wire components.
 * <br>
 * - Power from other redstone wires.
 * <br>
 * We can combine the two to reduce calls to World.getBlockState and BlockState.isSolidBlock
 * as well as calls to BlockState.getWeakRedstonePower and BlockState.getStrongRedstonePower.
 * We can avoid calling those last two methods on redstone wires altogether, since we know
 * they should return 0.
 * <br>
 * These changes can lead to a mspt reduction of up to 30% on top of Lithium's other
 * performance improvements.
 * 
 * @author Space Walker
 */
@Mixin(RedstoneWireBlock.class)
public class RedstoneWireBlockMixin extends Block {
    
    private static final int MIN = 0;            // smallest possible power value
    private static final int MAX = 15;           // largest possible power value
    private static final int MAX_WIRE = MAX - 1; // largest possible power a wire can receive from another wire
    
    public RedstoneWireBlockMixin(Settings settings) {
        super(settings);
    }
    
    @Inject(
            method = "getReceivedRedstonePower",
            cancellable = true,
            at = @At(
                    value = "HEAD"
            )
    )
    private void getReceivedPowerFaster(World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.getReceivedPower(world, pos));
    }
    
    /**
     * Calculate the redstone power a wire at the given location receives from the
     * blocks around it.
     */
    private int getReceivedPower(World world, BlockPos pos) {
        WorldChunk chunk = world.getWorldChunk(pos);
        int power = MIN;
        
        for (Direction dir : DirectionConstants.VERTICAL) {
            BlockPos side = pos.offset(dir);
            BlockState neighbor = chunk.getBlockState(side);
            
            // Wires do not accept power from other wires directly above or below them,
            // so those can be ignored. Similarly, if there is air directly above or
            // below a wire, it does not receive any power from that direction.
            if (!neighbor.isAir() && !neighbor.isOf(this)) {
                power = Math.max(power, this.getPowerFromVertical(world, side, neighbor, dir));
                
                if (power >= MAX) {
                    return MAX;
                }
            }
        }
        
        // In vanilla this check is done up to 4 times.
        BlockPos up = pos.up();
        boolean checkWiresAbove = !chunk.getBlockState(up).isSolidBlock(world, up);
        
        for (Direction dir : DirectionConstants.HORIZONTAL) {
            power = Math.max(power, this.getPowerFromSide(world, pos.offset(dir), dir, checkWiresAbove));
            
            if (power >= MAX) {
                return MAX;
            }
        }
        
        return power;
    }
    
    /**
     * Calculate the redstone power a wire receives from a block above or below it.
     * We do these positions separately because there are no wire connections
     * vertically. This simplifies the calculations a little.
     */
    private int getPowerFromVertical(World world, BlockPos pos, BlockState state, Direction toDir) {
        int power = state.getWeakRedstonePower(world, pos, toDir);
        
        if (power >= MAX) {
            return MAX;
        }
        
        if (state.isSolidBlock(world, pos)) {
            return Math.max(power, this.getStrongPowerTo(world, pos, toDir.getOpposite()));
        }
        
        return power;
    }
    
    /**
     * Calculate the redstone power a wire receives from blocks next to it.
     */
    private int getPowerFromSide(World world, BlockPos pos, Direction toDir, boolean checkWiresAbove) {
        WorldChunk chunk = world.getWorldChunk(pos);
        BlockState state = chunk.getBlockState(pos);
        
        if (state.isOf(this)) {
            return state.get(Properties.POWER) - 1;
        }
        
        int power = state.getWeakRedstonePower(world, pos, toDir);
        
        if (power >= MAX) {
            return MAX;
        }
        
        if (state.isSolidBlock(world, pos)) {
            power = Math.max(power, this.getStrongPowerTo(world, pos, toDir.getOpposite()));
            
            if (power >= MAX) {
                return MAX;
            }
            
            if (checkWiresAbove && power < MAX_WIRE) {
                BlockPos up = pos.up();
                BlockState aboveState = chunk.getBlockState(up);
                
                if (aboveState.isOf(this)) {
                    power = Math.max(power, aboveState.get(Properties.POWER) - 1);
                }
            }
        } else if (power < MAX_WIRE) {
            BlockPos down = pos.down();
            BlockState belowState = chunk.getBlockState(down);
            
            if (belowState.isOf(this)) {
                power = Math.max(power, belowState.get(Properties.POWER) - 1);
            }
        }
        
        return power;
    }
    
    /**
     * Calculate the strong power a block receives from the blocks around it.
     */
    private int getStrongPowerTo(World world, BlockPos pos, Direction ignore) {
        int power = MIN;
        
        for (Direction dir : DirectionConstants.ALL) {
            if (dir != ignore) {
                BlockPos side = pos.offset(dir);
                BlockState neighbor = world.getBlockState(side);
                
                if (!neighbor.isAir() && !neighbor.isOf(this)) {
                    power = Math.max(power, neighbor.getStrongRedstonePower(world, side, dir));
                    
                    if (power >= MAX) {
                        return MAX;
                    }
                }
            }
        }
        
        return power;
    }
}
