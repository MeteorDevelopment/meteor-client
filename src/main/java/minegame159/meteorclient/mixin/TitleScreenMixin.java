/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import com.g00fy2.versioncompare.Version;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.gui.GuiThemes;
import minegame159.meteorclient.gui.screens.NewUpdateScreen;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.network.HttpUtils;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

            MeteorExecutor.execute(() -> HttpUtils.getLines("http://meteorclient.com/api/version", s -> {
                Version latestVer = new Version(s);
                if (latestVer.isHigherThan(Config.get().version)) MinecraftClient.getInstance().openScreen(new NewUpdateScreen(GuiThemes.get(), latestVer));
            }));
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
