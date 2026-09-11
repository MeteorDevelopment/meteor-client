package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.mixininterface.IServerComponent;
import meteordevelopment.meteorclient.utils.network.KeyResolutionProtection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(KeybindContents.class)
public abstract class KeybindContentsMixin implements IServerComponent {
    @Shadow
    @Final
    private String name;

    @Unique
    private boolean ember$fromServer;

    @Override
    public void ember$markFromServer() {
        ember$fromServer = true;
    }

    @Override
    public boolean ember$isFromServer() {
        return ember$fromServer;
    }

    @WrapOperation(
        method = "getNestedComponent",
        at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;")
    )
    private Object ember$protectKeybind(Supplier<?> supplier, Operation<Object> original) {
        Object resolved = original.call(supplier);
        return ember$fromServer ? KeyResolutionProtection.keybind(name, (Component) resolved) : resolved;
    }
}
