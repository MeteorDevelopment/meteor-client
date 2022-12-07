package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.entity.EntityType;
import net.minecraft.network.listener.ClientPlayPacketListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket.class)
public class EntitySpawnS2CPacket {
    @Shadow
    @Final
    private EntityType<?> entityTypeId;

    @Inject(method = "apply(Lnet/minecraft/network/listener/ClientPlayPacketListener;)V", at = @At("HEAD"), cancellable = true)
    private void apply(ClientPlayPacketListener clientPlayPacketListener, CallbackInfo ci) {
        if (this.entityTypeId != null && Modules.get().get(NoRender.class).noEntity(this.entityTypeId)) ci.cancel();
    }
}
