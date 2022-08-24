/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.AutoBrewer;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BrewingStandScreen.class)
public abstract class BrewingStandScreenMixin extends HandledScreen<BrewingStandScreenHandler> {
    public BrewingStandScreenMixin(BrewingStandScreenHandler container, PlayerInventory playerInventory, Text name) {
        super(container, playerInventory, name);
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();

        if (Modules.get().isActive(AutoBrewer.class)) Modules.get().get(AutoBrewer.class).tick(handler);
    }

    @Override
    public void close() {
        if (Modules.get().isActive(AutoBrewer.class)) Modules.get().get(AutoBrewer.class).onBrewingStandClose();

        super.close();
    }
}
