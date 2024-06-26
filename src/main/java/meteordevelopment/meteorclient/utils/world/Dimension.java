/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

public enum Dimension {
    Overworld,
    Nether,
    End;

    public Dimension opposite() {
        return switch (this) {
            case Overworld -> Nether;
            case Nether -> Overworld;
            default -> this;
        };
    }
}
