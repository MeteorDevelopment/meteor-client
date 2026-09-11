package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.mixininterface.IServerComponent;
import meteordevelopment.meteorclient.utils.network.KeyResolutionProtection;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TranslatableContents.class)
public abstract class TranslatableContentsMixin implements IServerComponent {
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
        method = "decompose",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;)Ljava/lang/String;")
    )
    private String ember$protectKey(Language language, String key, Operation<String> original) {
        String resolved = original.call(language, key);
        return ember$fromServer ? KeyResolutionProtection.translation(key, resolved, key) : resolved;
    }

    @WrapOperation(
        method = "decompose",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")
    )
    private String ember$protectKeyWithFallback(Language language, String key, String fallback, Operation<String> original) {
        String resolved = original.call(language, key, fallback);
        return ember$fromServer ? KeyResolutionProtection.translation(key, resolved, fallback) : resolved;
    }
}
