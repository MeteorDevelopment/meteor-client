package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkDeltaUpdateS2CPacket.class)
public class ChunkDeltaUpdateS2CPacketMixin implements IChunkDeltaUpdateS2CPacket {
    @Shadow private ChunkPos chunkPos;

    @Override
    public ChunkPos getChunkPos() {
        return chunkPos;
    }
}
