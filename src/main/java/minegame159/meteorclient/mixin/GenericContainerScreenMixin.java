/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.world.AutoSteal;
import minegame159.meteorclient.utils.render.MeteorButtonWidget;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
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

        AutoSteal autoSteal = Modules.get().get(AutoSteal.class);

        if (autoSteal.isActive() && autoSteal.getStealButtonEnabled())
            addButton(new MeteorButtonWidget(x + backgroundWidth - 88, y + 3, 40, 12, new LiteralText("Steal"), button -> steal(handler)));
        if (autoSteal.isActive() && autoSteal.getDumpButtonEnabled())
            addButton(new MeteorButtonWidget(x + backgroundWidth - 46, y + 3, 40, 12, new LiteralText("Dump"), button -> dump(handler)));

        if (autoSteal.isActive() && autoSteal.getAutoStealEnabled()) steal(handler);
        else if (autoSteal.isActive() && autoSteal.getAutoDumpEnabled()) dump(handler);
    }

    private void steal(GenericContainerScreenHandler handler) {
        Modules.get().get(AutoSteal.class).stealAsync(handler);
    }

    private void dump(GenericContainerScreenHandler handler) {
        Modules.get().get(AutoSteal.class).dumpAsync(handler);
    }
}
