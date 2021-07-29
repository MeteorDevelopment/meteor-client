/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Version;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.PromptBuilder;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.util.Util;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    private final int WHITE = Color.fromRGBA(255, 255, 255, 255);
    private final int GRAY = Color.fromRGBA(175, 175, 175, 255);

    private String text1;
    private int text1Length;

    private String text2;
    private int text2Length;

    private String text3;
    private int text3Length;

    private String text4;
    private int text4Length;

    private String text5;
    private int text5Length;

    private String text6;

    private int fullLength;
    private int prevWidth;

    public TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {

        text1 = "Meteor Client by ";
        text2 = "MineGame159";
        text3 = ", ";
        text4 = "squidoodly";
        text5 = " & ";
        text6 = "seasnail";

        text1Length = textRenderer.getWidth(text1);
        text2Length = textRenderer.getWidth(text2);
        text3Length = textRenderer.getWidth(text3);
        text4Length = textRenderer.getWidth(text4);
        text5Length = textRenderer.getWidth(text5);
        int text6Length = textRenderer.getWidth(text6);

        fullLength = text1Length + text2Length + text3Length + text4Length + text5Length + text6Length;
        prevWidth = 0;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;drawStringWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", ordinal = 0))
    private void onRenderIdkDude(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (Utils.firstTimeTitleScreen) {
            Utils.firstTimeTitleScreen = false;
            MeteorClient.LOG.info("Checking latest version of Meteor Client");

            MeteorExecutor.execute(() -> {
                String res = Http.get("https://meteorclient.com/api/version").sendString();
                if (res == null) return;

                Version latestVer = new Version(res);

                if (latestVer.isHigherThan(Config.get().version)) {
                    new PromptBuilder()
                        .title("New Update")
                        .message("A new version of Meteor has been released.")
                        .appendLine(String.format("Your version: %s", Config.get().version.toString()))
                        .appendLine(String.format("Latest version: %s", latestVer.toString()))
                        .appendLine("Do you want to update?")
                        .onYes(() -> {
                            Util.getOperatingSystem().open("https://meteorclient.com/");
                        })
                        .promptId("new-update")
                        .show();
                }
            });
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (!Config.get().titleScreenCredits) return;
        prevWidth = 0;
        textRenderer.drawWithShadow(matrices, text1, width - fullLength - 3, 3, WHITE);
        prevWidth += text1Length;
        textRenderer.drawWithShadow(matrices, text2, width - fullLength + prevWidth - 3, 3, GRAY);
        prevWidth += text2Length;
        textRenderer.drawWithShadow(matrices, text3, width - fullLength + prevWidth - 3, 3, WHITE);
        prevWidth += text3Length;
        textRenderer.drawWithShadow(matrices, text4, width - fullLength + prevWidth - 3, 3, GRAY);
        prevWidth += text4Length;
        textRenderer.drawWithShadow(matrices, text5, width - fullLength + prevWidth - 3, 3, WHITE);
        prevWidth += text5Length;
        textRenderer.drawWithShadow(matrices, text6, width - fullLength + prevWidth - 3, 3, GRAY);
    }
}
