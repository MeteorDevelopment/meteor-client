/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IDisconnectedScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin implements IDisconnectedScreen {
    @Shadow @Final private Screen parent;

    @Shadow @Final private Text reason;

    @Shadow private int reasonHeight;

    @Override
    public Screen getParent() {
        return parent;
    }

    @Override
    public Text getReason() {
        return reason;
    }

    @Override
    public int getReasonHeight() {
        return reasonHeight;
    }
}
