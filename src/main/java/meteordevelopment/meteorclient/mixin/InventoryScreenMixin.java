/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IDropItems;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author NDev007
 * @since 27.06.2025
 */

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends RecipeBookScreen<PlayerScreenHandler> implements IDropItems {

    public InventoryScreenMixin(PlayerScreenHandler handler, RecipeBookWidget<?> recipeBook, PlayerInventory inventory, Text title) {
        super(handler, recipeBook, inventory, title);
    }

    @Override
    public void dropItems() {
        if (this.client == null || this.client.interactionManager == null || this.client.player == null) {
            return;
        }

        for (int i = 0; i < this.handler.slots.size(); ++i) {
            if (this.client.currentScreen != this) {
                break;
            }
            this.client.interactionManager.clickSlot(this.handler.syncId, i, 0, SlotActionType.PICKUP, this.client.player);
            this.client.interactionManager.clickSlot(this.handler.syncId, -999, 0, SlotActionType.PICKUP, this.client.player);
        }
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/RecipeBookScreen;init()V", shift = At.Shift.AFTER))
    private void onInit(CallbackInfo ci) {
        InventoryTweaks invTweaks = Modules.get().get(InventoryTweaks.class);

        if (invTweaks.isActive() && invTweaks.showButtons()){
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Drop all"),
                    (button) -> {
                        this.dropItems();
                    }
                )
                .dimensions(this.width / 2 - 32, this.height / 2 - 110, 64, 20)
                .build());
        }
    }
}
