package meteordevelopment.meteorclient.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface VehicleMoveC2SPacketAccessor {
    @Mutable
    @Accessor("x")
    void setX(double x);

    @Mutable
    @Accessor("z")
    void setZ(double z);
}
