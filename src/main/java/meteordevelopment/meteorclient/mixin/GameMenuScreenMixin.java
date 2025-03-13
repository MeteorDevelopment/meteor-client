package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {
    @Unique
    private static final int clicks = 2;
    @Shadow
    private ButtonWidget exitButton;
    @Unique
    private int confirm = 0;

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void disconnect(final CallbackInfo ci) {
        if (confirm++ < clicks) {
            exitButton.active = true;
            exitButton.setMessage(Text.of("Confirm? (" + confirm + "/" + clicks + ")"));
            ci.cancel();
        }
    }
}
