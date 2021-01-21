/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.render.MeteorButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> implements ScreenHandlerProvider<GenericContainerScreenHandler> {
    public GenericContainerScreenMixin(GenericContainerScreenHandler container, PlayerInventory playerInventory, Text name) {
        super(container, playerInventory, name);
    }

    @Override
    protected void init() {
        super.init();

        //StorageUtils
        addButton(new MeteorButtonWidget(x + backgroundWidth - 46, y + 3, 40, 12, new LiteralText("Steal"), button -> steal(handler)));
        addButton(new MeteorButtonWidget(x + backgroundWidth - 88, y + 3, 40, 12, new LiteralText("Dump"), button -> dump(handler)));
    }

    private void steal(GenericContainerScreenHandler handler) {
        for (int i = 0; i < handler.getRows() * 9; i++) {
            InvUtils.clickSlot(i, 0, SlotActionType.QUICK_MOVE);
        }

        boolean empty = true;
        for (int i = 0; i < handler.getRows() * 9; i++) {
            if (!handler.getSlot(i).getStack().isEmpty()) {
                empty = false;
                break;
            }
        }

        if (empty) MinecraftClient.getInstance().player.closeHandledScreen();
    }

    private void dump(GenericContainerScreenHandler handler) {
        for (int i = handler.getRows() * 9; i < handler.getRows() * 9 + 1 + 3 * 9; i++) {
            InvUtils.clickSlot(i, 0, SlotActionType.QUICK_MOVE);
        }
    }
}
