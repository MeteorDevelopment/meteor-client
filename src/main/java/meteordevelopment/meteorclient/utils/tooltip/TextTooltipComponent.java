/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class TextTooltipComponent extends ClientTextTooltip implements MeteorTooltipData {
    public TextTooltipComponent(FormattedCharSequence text) {
        super(text);
    }

    public TextTooltipComponent(Component text) {
        this(text.getVisualOrderText());
    }

    @Override
    public ClientTooltipComponent getComponent() {
        return this;
    }
}
