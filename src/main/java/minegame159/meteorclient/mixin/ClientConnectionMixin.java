package minegame159.meteorclient.mixin;

import io.netty.channel.Channel;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.modules.ModuleManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Shadow private Channel channel;

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(Text disconnectReason, CallbackInfo info) {
        if (channel.isOpen()) {
            MeteorClient.saveConfig();
            ModuleManager.deactivateAll();
        }
    }
}
