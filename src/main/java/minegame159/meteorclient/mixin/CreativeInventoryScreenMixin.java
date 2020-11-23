/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.ICreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CreativeInventoryScreen.class)
public class CreativeInventoryScreenMixin implements ICreativeInventoryScreen {
    @Shadow private static int selectedTab;

    @Override
    public int getSelectedTab() {
        return selectedTab;
    }
}
