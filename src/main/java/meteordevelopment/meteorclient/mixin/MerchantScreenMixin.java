/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.QuickTrade;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends HandledScreen<MerchantScreenHandler> {
    @Shadow
    private int selectedIndex;

    public MerchantScreenMixin(MerchantScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(
        method = "render",
        at = @At("TAIL")
    )
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        if (client == null) {
            return;
        }

        QuickTrade module = Modules.get().get(QuickTrade.class);


        if (!module.isActive()) {
            return;
        }

        if (!module.modifier.get().isPressed()) {
            return;
        }

        context.drawCenteredTextWithShadow(client.textRenderer, "Select a trade on the left to quick-trade", width / 2, height / 2 + 100, 0xFFFFFFFF);
    }

    @Inject(
        method = "syncRecipeIndex",
        at = @At("TAIL")
    )
    private void syncRecipeIndex(CallbackInfo ci) {
        // Called when a new trade is selected, server is notified at end of call

        QuickTrade module = Modules.get().get(QuickTrade.class);

        if (!module.isActive()) {
            return;
        }

        if (!module.modifier.get().isPressed()) {
            return;
        }

        TradeOfferList tradeOfferList = this.handler.getRecipes();
        if (tradeOfferList.size() < selectedIndex) return;

        TradeOffer selectedOffer = tradeOfferList.get(selectedIndex);
        MerchantScreenHandler handler = getScreenHandler();
        module.trade(selectedOffer, handler, this.selectedIndex);
    }
}

