/*
 * 这里的 package 路径要根据你放的位置定
 */
package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;

public class AntiPacketKick extends Module {
    public AntiPacketKick() {
        super(Categories.Misc, "anti-packet-kick", "拦截 Fabric 和 Loader 的握手包，防止被服务器检测到模组列表。");
    }

  @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof CustomPayloadC2SPacket packet) {
            // 获取 payload 对象
            var payload = packet.payload();
            
            // 【关键修改点】
            // 尝试输入 payload. 后看 IDE 提示。
            // 可能性 1 (Yarn 1.21+): payload.getId().id()
            // 可能性 2 (旧版 Yarn): payload.getId()
            // 可能性 3 (MojMap): payload.type().id()
            
            // 这里假设是最新版 Meteor (1.21 Yarn):
            net.minecraft.util.Identifier id = payload.getId().id();

            String namespace = id.getNamespace();
            String path = id.getPath();

            // 1. 拦截 Fabric
            if (namespace.equals("fabric")) {
                event.cancel();
                return;
            }

            // 2. 拦截 Forge/FML (可选)
            if (namespace.equals("fml") || namespace.equals("forge")) {
                event.cancel();
            }
        }
    }

}