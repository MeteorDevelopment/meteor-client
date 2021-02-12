/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatHudLine.class)
public interface ChatHudLineAccessor<T> {
    @Accessor("creationTick")
    void setTimestamp(int timestamp);

    @Accessor("text")
    void setText(T text);

    @Accessor("id")
    void setId(int id);
}
