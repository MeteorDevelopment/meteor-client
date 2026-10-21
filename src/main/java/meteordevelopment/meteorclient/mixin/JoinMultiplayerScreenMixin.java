/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
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
    private static final int BUTTON_WIDTH = 75;
    @Unique
    private static final int BUTTON_HEIGHT = 20;
    @Unique
    private static final int MARGIN = 3;

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
                Button.builder(Component.literal("Accounts"), _ -> minecraft.gui.setScreen(GuiThemes.get().accountsScreen()))
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build()
            );
        }

        Config config = Config.get();
        Config.ButtonPosition accountPos = config.accountButtonAnchor.get();
        boolean accountsVisible = accountPos != Config.ButtonPosition.Hidden;

        // 26.4: AbstractWidget#visible became protected, so it is accessed through the accessor.
        ((AbstractWidgetAccessor) accounts).meteor$setVisible(accountsVisible);

        positionButton(accounts, accountPos);
    }

    @Unique
    private void positionButton(Button button, Config.ButtonPosition anchor) {
        switch (anchor) {
            case TopRight    -> button.setPosition(this.width  - MARGIN - BUTTON_WIDTH, MARGIN);
            case TopLeft     -> button.setPosition(MARGIN, MARGIN);
            case BottomLeft  -> button.setPosition(MARGIN, this.height - MARGIN - BUTTON_HEIGHT);
            case BottomRight -> button.setPosition(this.width  - MARGIN - BUTTON_WIDTH, this.height - MARGIN - BUTTON_HEIGHT);
            default -> button.setPosition(this.width  - MARGIN - BUTTON_WIDTH, MARGIN); // Top right
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);

        Config config = Config.get();

        if (!config.showAccountStatus.get()) {
            return;
        }

        // Shifts the top left account text to the right if the button is also top left
        int x = MARGIN;
        if (config.accountButtonAnchor.get() != Config.ButtonPosition.Hidden && config.accountButtonAnchor.get() == Config.ButtonPosition.TopLeft) {
            x += BUTTON_WIDTH;
        }

        int y = MARGIN;

        graphics.text(mc.font, loggedInAs, x, y, textColor1);
        graphics.text(mc.font, Modules.get().get(NameProtect.class).getName(minecraft.getUser().getName()), x + loggedInAsLength, y, textColor2);
    }
}
