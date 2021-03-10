package minegame159.meteorclient.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.options.SkinOptionsScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(SkinOptionsScreen.class)
public class SkinOptionsScreenMixin extends ScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo callback) {
        ButtonWidget btn1 = (ButtonWidget) buttons.get(0);
        ButtonWidget btn2 = (ButtonWidget) buttons.get(1);

        int width = btn2.x - btn1.x + btn2.getWidth();

        // reorder buttons
        List<AbstractButtonWidget> btns = new ArrayList<>(buttons);
        buttons.clear();

        addButton(new ButtonWidget(btn1.x, btn1.y - 24, width, 20, Text.of("Open OptiFine Cape Editor"), btn -> {
            Session sess = client.getSession();
            GameProfile profile = sess.getProfile();

            String name = profile.getName();
            String uuid = profile.getId().toString().replace("-", "");
            String token = sess.getAccessToken();

            String serverID = new BigInteger(128, new Random())
                    .xor(new BigInteger(128, new Random(System.identityHashCode(new Object()))))
                    .toString(16);

            try {
                client.getSessionService().joinServer(profile, token, serverID);
                String url = String.format("https://optifine.net/capeChange?u=%s&n=%s&s=%s", uuid, name, serverID);

                client.openScreen(new ConfirmChatLinkScreen(cb -> {
                    if (cb) {
                        Util.getOperatingSystem().open(url);
                    }
                    client.openScreen((SkinOptionsScreen) (Object) this);
                }, url, true));
            } catch (AuthenticationException e) {
                StringBuilder sb = new StringBuilder("§cCould not generate Cape Editor Session: §7");
                sb.append(e.getMessage());
                if (e.getCause() != null)
                    sb.append(" (").append(e.getCause().getMessage()).append(")");
                ChatUtils.warning(sb.toString());

                e.printStackTrace();
            }
        }));

        buttons.addAll(btns);
        buttons.forEach(btn -> btn.y += 24); // move everything 24 units down
    }
}
