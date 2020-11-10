package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.ICloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CloseHandledScreenC2SPacket.class)
public class CloseHandledScreenC2SPacketMixin implements ICloseHandledScreenC2SPacket {
    @Shadow private int syncId;

    @Override
    public int getSyncId() {
        return syncId;
    }
}
