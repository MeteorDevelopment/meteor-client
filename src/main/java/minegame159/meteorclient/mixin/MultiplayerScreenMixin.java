package minegame159.meteorclient.mixin;

import minegame159.meteorclient.accountsfriends.AccountsScreen;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {
    private int textColor1;
    private int textColor2;

    private String loggedInAs;
    private int loggedInAsLength;

    public MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        textColor1 = Color.fromRGBA(255, 255, 255, 255);
        textColor2 = Color.fromRGBA(175, 175, 175, 255);

        loggedInAs = "Logged in as ";

        loggedInAsLength = font.getStringWidth(loggedInAs);

        addButton(new ButtonWidget(this.width - 75 - 3, 3, 75, 20, "Accounts", button -> {
            minecraft.openScreen(new AccountsScreen());
        }));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(int mouseX, int mouseY, float delta, CallbackInfo info) {
        drawString(font, loggedInAs, 3, 3, textColor1);
        drawString(font, minecraft.getSession().getUsername(), 3 + loggedInAsLength, 3, textColor2);
    }
}
