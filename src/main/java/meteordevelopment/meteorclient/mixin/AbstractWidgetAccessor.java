/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.class)
public interface AbstractWidgetAccessor {
    /**
     * {@code visible} was public up to 26.3 and became protected in 26.4.
     */
    @Accessor("visible")
    boolean meteor$isVisible();

    @Accessor("visible")
    void meteor$setVisible(boolean visible);
}
