package minegame159.meteorclient.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface IMinecraftClient {
    @Invoker(value = "doAttack")
    public void leftClick();

    @Invoker(value = "doItemUse")
    public void rightClick();

}
