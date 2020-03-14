package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    private int text1Color = Color.fromRGBA(255, 255, 255, 255);
    private int text2Color = Color.fromRGBA(175, 175, 175, 255);

    private String text1 = "Meteor Client by ";
    private int text1Length;

    private String text2 = "MineGame159";
    private int text2Length;

    public TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        text1Length = font.getStringWidth(text1);
        text2Length = font.getStringWidth(text2);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(int mouseX, int mouseY, float delta, CallbackInfo info) {
        drawString(font, text1, width - text2Length - text1Length - 3, 3, text1Color);
        drawString(font, text2, width - text2Length - 3, 3, text2Color);
    }
}
