package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientCommonNetworkHandler.class)
public class PreventChannelLeakMixin {

    private static final List<String> BLACKLISTED_CHANNELS = List.of(
        "meteor",
        "baritone"
    );

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof CustomPayloadC2SPacket payloadPacket) {
            
            // 修复点：使用 .getId().id()
            // .getId() 获取 CustomPayload.Id 对象
            // .id() 获取 Identifier
            Identifier id = payloadPacket.payload().getId().id();
            
            String channelName = id.toString().toLowerCase();

            for (String blocked : BLACKLISTED_CHANNELS) {
                if (channelName.contains(blocked)) {
                    System.out.println("[Anti-Leak] 拦截通道: " + channelName);
                    ci.cancel();
                    return;
                }
            }
        }
    }
}