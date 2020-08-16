package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IGuiCloseC2SPacket;
import net.minecraft.network.packet.c2s.play.GuiCloseC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiCloseC2SPacket.class)
public class GuiCloseC2SPacketMixin implements IGuiCloseC2SPacket {
    @Shadow private int id;

    @Override
    public int getSyncId() {
        return id;
    }
}
