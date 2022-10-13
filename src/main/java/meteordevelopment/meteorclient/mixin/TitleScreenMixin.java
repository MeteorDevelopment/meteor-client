/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.google.gson.JsonParser;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.TitleScreenCredits;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import meteordevelopment.meteorclient.utils.render.prompts.YesNoPrompt;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    public TitleScreenMixin(Text title) {
        super(title);
    }

    private static final Identifier METEOR_BTN = new MeteorIdentifier("textures/gui/meteor_btn.png");

    @Inject(method = "init()V", at = @At("HEAD"))
    private void init(CallbackInfo info) {
        addDrawableChild(new TexturedButtonWidget( width / 2 - 124, height/4 + 72,
            20, 20, 0, 0, 20, METEOR_BTN, 20, 40, b -> {
            GuiTheme theme = GuiThemes.get();
            TabScreen screen = Tabs.get().get(0).createScreen(theme);
            screen.addDirect(theme.topBar()).top().centerX();
            client.setScreen(screen);
        }));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawStringWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", ordinal = 0))
    private void onRenderIdkDude(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (Utils.firstTimeTitleScreen) {
            Utils.firstTimeTitleScreen = false;
            MeteorClient.LOG.info("Checking latest version of Meteor Client");

            MeteorExecutor.execute(() -> {
                String res = Http.get("https://meteorclient.com/api/stats").sendString();
                if (res == null) return;

                Version latestVer = new Version(JsonParser.parseString(res).getAsJsonObject().get("version").getAsString());

                if (latestVer.isHigherThan(MeteorClient.VERSION)) {
                    YesNoPrompt.create()
                        .title("New Update")
                        .message("A new version of Meteor has been released.")
                        .message("Your version: %s", MeteorClient.VERSION)
                        .message("Latest version: %s", latestVer)
                        .message("Do you want to update?")
                        .onYes(() -> Util.getOperatingSystem().open("https://meteorclient.com/"))
                        .onNo(() -> OkPrompt.create()
                            .title("Are you sure?")
                            .message("Using old versions of Meteor is not recommended")
                            .message("and could report in issues.")
                            .id("new-update-no")
                            .onOk(this::close)
                            .show())
                        .id("new-update")
                        .show();
                }
            });
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (Config.get().titleScreenCredits.get()) TitleScreenCredits.render(matrices);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
        if (Config.get().titleScreenCredits.get() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (TitleScreenCredits.onClicked(mouseX, mouseY)) info.setReturnValue(true);
        }
    }
}
