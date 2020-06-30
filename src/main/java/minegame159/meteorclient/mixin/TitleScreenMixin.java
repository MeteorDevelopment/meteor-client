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
    private int text1Color;
    private int text2Color;
    private int text3Color;
    private int text4Color;

    private String text1;
    private int text1Length;

    private String text2;
    private int text2Length;

    private String text3;
    private int text3Length;

    private String text4;
    private int text4Length;

    public TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        text1Color = Color.fromRGBA(255, 255, 255, 255);
        text2Color = Color.fromRGBA(175, 175, 175, 255);
        text3Color = Color.fromRGBA(255, 255, 255, 255);
        text4Color = Color.fromRGBA(175, 175, 175, 255);

        text1 = "Meteor Client by ";
        text2 = "MineGame159";
        text3 = " & ";
        text4 = "squidoodly";

        text1Length = font.getStringWidth(text1);
        text2Length = font.getStringWidth(text2);
        text3Length = font.getStringWidth(text3);
        text4Length = font.getStringWidth(text4);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(int mouseX, int mouseY, float delta, CallbackInfo info) {
        drawString(font, text1, width - text4Length - text3Length - text2Length - text1Length - 3, 3, text1Color);
        drawString(font, text2, width - text4Length - text3Length - text2Length - 3, 3, text2Color);
        drawString(font, text3, width - text4Length - text3Length - 3, 3, text3Color);
        drawString(font, text4, width - text4Length - 3, 3, text4Color);
    }
}
