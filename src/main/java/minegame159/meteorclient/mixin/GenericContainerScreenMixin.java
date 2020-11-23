/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.widget.ButtonWidget;
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

        // Steal
        addButton(new ButtonWidget(x + width - 50 - 7, y + 3, 50, 12, new LiteralText("Steal"), button -> {
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
        }));

        // Dump
        addButton(new ButtonWidget(x + width - 50 - 7, y + this.height - 96 - 1, 50, 12, new LiteralText("Dump"), button -> {
            for (int i = handler.getRows() * 9; i < handler.getRows() * 9 + 1 + 3 * 9; i++) {
                InvUtils.clickSlot(i, 0, SlotActionType.QUICK_MOVE);
            }
        }));
    }
}
