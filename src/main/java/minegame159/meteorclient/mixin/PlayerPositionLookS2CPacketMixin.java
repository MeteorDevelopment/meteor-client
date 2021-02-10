package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IPlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerPositionLookS2CPacket.class)
public class PlayerPositionLookS2CPacketMixin implements IPlayerPositionLookS2CPacket {
    @Shadow protected float yaw;
    @Shadow protected float pitch;

    @Override
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    @Override
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
