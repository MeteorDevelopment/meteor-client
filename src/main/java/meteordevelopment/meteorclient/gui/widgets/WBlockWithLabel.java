/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.block.BlockState;

public class WBlockWithLabel extends WHorizontalList {
    private BlockState state;
    private String name;

    private WBlock block;
    private WLabel label;

    public WBlockWithLabel(BlockState state, String name) {
        this.state = state;
        this.name = name;
    }

    @Override
    public void init() {
        block = add(theme.block(state)).widget();
        label = add(theme.label(name)).widget();
    }

    public void set(BlockState state) {
        this.state = state;
        block.state = state;

        name = Names.get(state.getBlock());
        label.set(name);
    }

    public String getLabelText() {
        return label == null ? name : label.get();
    }
}
