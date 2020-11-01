package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Pool;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends ToggleModule {
    public enum Mode {
        All,
        Flatten,
        Smash
    }

    public enum SortMode {
        None,
        Closest,
        Furthest
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Which blocks to break.")
            .defaultValue(Mode.All)
            .build()
    );

    private final Setting<Boolean> packetMine = sgGeneral.add(new BoolSetting.Builder()
            .name("packet-mine")
            .description("Mines blocks using packet spamming.")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Block>> selectedBlocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("selected-blocks")
            .description("Which blocks to mine when only selected is true.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> onlySelected = sgGeneral.add(new BoolSetting.Builder()
            .name("only-selected")
            .description("Only mines selected blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Break range.")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
            .name("sort-mode")
            .description("Which blocks to mine first.")
            .defaultValue(SortMode.Closest)
            .build()
    );

    private final Setting<Boolean> noParticles = sgGeneral.add(new BoolSetting.Builder()
            .name("no-particles")
            .description("Disables block break particles.")
            .defaultValue(false)
            .build()
    );

    private final Pool<BlockPos.Mutable> blockPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> blocks = new ArrayList<>();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final BlockPos.Mutable lastBlockPos = new BlockPos.Mutable();
    private boolean hasLastBlockPos;

    public Nuker() {
        super(Category.Misc, "nuker", "Breaks blocks around you.");
    }

    @Override
    public void onDeactivate() {
        mc.interactionManager.cancelBlockBreaking();
        hasLastBlockPos = false;
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (hasLastBlockPos && mc.world.getBlockState(lastBlockPos).getBlock() != Blocks.AIR) {
            mc.interactionManager.updateBlockBreakingProgress(lastBlockPos, Direction.UP);
            return;
        }

        hasLastBlockPos = false;

        // Calculate stuff
        double pX = mc.player.getX() - 0.5;
        double pY = mc.player.getY();
        double pZ = mc.player.getZ() - 0.5;

        int minX = (int) Math.floor(pX - range.get());
        int minY = (int) Math.floor(pY - range.get());
        int minZ = (int) Math.floor(pZ - range.get());

        int maxX = (int) Math.floor(pX + range.get());
        int maxY = (int) Math.floor(pY + range.get());
        int maxZ = (int) Math.floor(pZ + range.get());

        double rangeSq = Math.pow(range.get(), 2);

        // Find blocks to break
        for (int y = minY; y <= maxY; y++) {
            boolean skipThisYLevel = false;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (Utils.squaredDistance(pX, pY, pZ, x, y, z) > rangeSq) continue;
                    blockPos.set(x, y, z);
                    BlockState state = mc.world.getBlockState(blockPos);
                    if (state.getOutlineShape(mc.world, blockPos) == VoxelShapes.empty()) continue;

                    // Flatten
                    if (mode.get() == Mode.Flatten && y < mc.player.getY()) {
                        skipThisYLevel = true;
                        break;
                    }

                    // Smash
                    if (mode.get() == Mode.Smash && state.getHardness(mc.world, blockPos) != 0) continue;

                    // Only selected
                    if (onlySelected.get() && !selectedBlocks.get().contains(state.getBlock())) continue;

                    BlockPos.Mutable pos = blockPool.get();
                    pos.set(x, y, z);
                    blocks.add(pos);
                }

                if (skipThisYLevel) break;
            }
        }

        // Sort blocks
        if (sortMode.get() != SortMode.None) {
            blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX(), value.getY(), value.getZ()) * (sortMode.get() == SortMode.Closest ? 1 : -1)));
        }

        // Break blocks
        boolean breaking = false;

        for (BlockPos.Mutable pos : blocks) {
            if (packetMine.get()) {
                // Packet mine
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
            } else {
                // Check last block
                if (!lastBlockPos.equals(pos)) {
                    mc.interactionManager.cancelBlockBreaking();
                    mc.interactionManager.attackBlock(pos, Direction.UP);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }

                // Break block
                lastBlockPos.set(pos);
                mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);

                breaking = true;
                hasLastBlockPos = true;
                break;
            }
        }

        if (!breaking) mc.interactionManager.cancelBlockBreaking();

        // Empty blocks list
        for (BlockPos.Mutable pos : blocks) blockPool.free(pos);
        blocks.clear();
    });

    public boolean noParticles() {
        return isActive() && noParticles.get();
    }
}
