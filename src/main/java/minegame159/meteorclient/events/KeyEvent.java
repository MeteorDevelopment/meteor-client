/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events;

import minegame159.meteorclient.utils.KeyAction;

public class KeyEvent extends Cancellable {
    public int key;
    public KeyAction action;
}
