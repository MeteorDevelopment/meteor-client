package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ClientBrandRetriever.class, priority = 1001)
public class ClientBrandRetrieverMixin {

    /**
     * @author MeteorUser
     * @reason 在模组服，必须伪装成普通的 fabric 客户端，而不是 vanilla
     */
    @Overwrite
    public static String getClientModName() {
        // 【关键修改】这里要返回 "fabric"，千万别写 "vanilla"
        // 这样服务器就知道你准备好接收模组数据包了
        return "fabric";
    }
}