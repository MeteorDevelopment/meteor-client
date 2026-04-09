/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    @Unique
    private int textColor1;
    @Unique
    private int textColor2;

    @Unique
    private String loggedInAs;
    @Unique
    private int loggedInAsLength;

    @Unique
    private Button accounts;

    @Unique
    private Button proxies;

    public JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        textColor1 = Color.fromRGBA(255, 255, 255, 255);
        textColor2 = Color.fromRGBA(175, 175, 175, 255);

        loggedInAs = "Logged in as ";
        loggedInAsLength = font.width(loggedInAs);

        if (accounts == null) {
            accounts = addRenderableWidget(
                new Button.Builder(Component.literal("Accounts"), _ -> minecraft.setScreen(GuiThemes.get().accountsScreen()))
                    .size(75, 20)
                    .build()
            );
        }
        accounts.setPosition(this.width - 75 - 3, 3);

        if (proxies == null) {
            proxies = addRenderableWidget(
                new Button.Builder(Component.literal("Proxies"), _ -> minecraft.setScreen(GuiThemes.get().proxiesScreen()))
                    .size(75, 20)
                    .build()
            );
        }
        proxies.setPosition(this.width - 75 - 3 - 75 - 2, 3);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);

        int x = 3;
        int y = 3;

        // Logged in as
        graphics.text(mc.font, loggedInAs, x, y, textColor1);
        graphics.text(mc.font, Modules.get().get(NameProtect.class).getName(minecraft.getUser().getName()), x + loggedInAsLength, y, textColor2);

        y += font.lineHeight + 2;

        // Proxy
        Proxy proxy = Proxies.get().getEnabled();

        String left = proxy != null ? "Using proxy " : "Not using a proxy";
        String right = proxy != null ? (proxy.name.get() != null && !proxy.name.get().isEmpty() ? "(" + proxy.name.get() + ") " : "") + proxy.address.get() + ":" + proxy.port.get() : null;

        graphics.text(mc.font, left, x, y, textColor1);
        if (right != null)
            graphics.text(mc.font, right, x + font.width(left), y, textColor2);
    }
}
