package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import meteordevelopment.meteorclient.utils.network.ServerComponentCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(ComponentSerialization.class)
public abstract class ComponentSerializationMixin {
    @WrapOperation(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;recursive(Ljava/lang/String;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;")
    )
    private static Codec<Component> ember$tagServerComponents(String name, Function<Codec<Component>, Codec<Component>> body, Operation<Codec<Component>> original) {
        return new ServerComponentCodec(original.call(name, body));
    }
}
