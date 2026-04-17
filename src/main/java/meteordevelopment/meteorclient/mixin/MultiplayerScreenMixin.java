/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import meteordevelopment.meteorclient.systems.proxies.Proxies;
import meteordevelopment.meteorclient.systems.proxies.Proxy;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Unique
    private int textColor1;
    @Unique
    private int textColor2;

    @Unique
    private String loggedInAs;
    @Unique
    private int loggedInAsLength;

    @Unique
    private ButtonWidget accounts;

    @Unique
    private ButtonWidget proxies;

    @Unique
    private static final int BUTTON_WIDTH = 75;
    @Unique
    private static final int BUTTON_HEIGHT = 20;
    @Unique
    private static final int MARGIN = 3;
    @Unique
    private static final int GAP = 2;

    public MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "refreshWidgetPositions", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        textColor1 = Color.fromRGBA(255, 255, 255, 255);
        textColor2 = Color.fromRGBA(175, 175, 175, 255);

        loggedInAs = "Logged in as ";
        loggedInAsLength = textRenderer.getWidth(loggedInAs);

        if (accounts == null) {
            accounts = addDrawableChild(
                new ButtonWidget.Builder(Text.literal("Accounts"), button -> client.setScreen(GuiThemes.get().accountsScreen()))
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build()
            );
        }

        if (proxies == null) {
            proxies = addDrawableChild(
                new ButtonWidget.Builder(Text.literal("Proxies"), button -> client.setScreen(GuiThemes.get().proxiesScreen()))
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build()
            );
        }

        Config config = Config.get();
        Config.ButtonPosition accountPos = config.accountButtonAnchor.get();
        Config.ButtonPosition proxiesPos = config.proxiesButtonAnchor.get();
        boolean accountsVisible = accountPos != Config.ButtonPosition.Hidden;
        boolean proxiesVisible = proxiesPos != Config.ButtonPosition.Hidden;

        accounts.visible = accountsVisible;
        proxies.visible = proxiesVisible;

        positionButton(accounts, accountPos, proxiesVisible && proxiesPos == accountPos, true);
        positionButton(proxies, proxiesPos, accountsVisible && accountPos == proxiesPos, false);
    }

    @Unique
    private void positionButton(ButtonWidget button, Config.ButtonPosition anchor, boolean sharingCorner, boolean isAccounts) {
        int leftOffset  = sharingCorner && isAccounts  ? BUTTON_WIDTH + GAP : 0;
        int rightOffset = sharingCorner && !isAccounts ? BUTTON_WIDTH + GAP : 0;

        switch (anchor) {
            case TopRight    -> button.setPosition(this.width  - MARGIN - BUTTON_WIDTH - rightOffset, MARGIN);
            case TopLeft     -> button.setPosition(MARGIN + leftOffset, MARGIN);
            case BottomLeft  -> button.setPosition(MARGIN + leftOffset, this.height - MARGIN - BUTTON_HEIGHT);
            case BottomRight -> button.setPosition(this.width  - MARGIN - BUTTON_WIDTH - rightOffset, this.height - MARGIN - BUTTON_HEIGHT);
            default -> button.setPosition(this.width  - MARGIN - BUTTON_WIDTH - rightOffset, MARGIN); // Top right
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        Config config = Config.get();

        if (!config.showAccountStatus.get() && !config.showProxiesStatus.get()) {
            return;
        }

        // Shifts the top left account and proxy text to right if buttons are also top left
        int x = MARGIN;
        if (config.proxiesButtonAnchor.get() != Config.ButtonPosition.Hidden && config.proxiesButtonAnchor.get() == Config.ButtonPosition.TopLeft) {
            x += BUTTON_WIDTH + GAP;
        }
        if (config.accountButtonAnchor.get() != Config.ButtonPosition.Hidden && config.accountButtonAnchor.get() == Config.ButtonPosition.TopLeft) {
            x += BUTTON_WIDTH + GAP;
        }

        int y = MARGIN;

        // Logged in as
        if (config.showAccountStatus.get()) {
            context.drawTextWithShadow(mc.textRenderer, loggedInAs, x, y, textColor1);
            context.drawTextWithShadow(mc.textRenderer, Modules.get().get(NameProtect.class).getName(client.getSession().getUsername()), x + loggedInAsLength, y, textColor2);

            y += textRenderer.fontHeight + 2;
        }

        if (!config.showProxiesStatus.get()) {
            return;
        }

        // Proxy
        Proxy proxy = Proxies.get().getEnabled();

        String left = proxy != null ? "Using proxy " : "Not using a proxy";
        String right = proxy != null ? (proxy.name.get() != null && !proxy.name.get().isEmpty() ? "(" + proxy.name.get() + ") " : "") + proxy.address.get() + ":" + proxy.port.get() : null;

        context.drawTextWithShadow(mc.textRenderer, left, x, y, textColor1);
        if (right != null)
            context.drawTextWithShadow(mc.textRenderer, right, x + textRenderer.getWidth(left), y, textColor2);
    }
}
