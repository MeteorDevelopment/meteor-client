/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.accounts.gui;

import minegame159.meteorclient.gui.widgets.WTextBox;


public class WAccountField extends WTextBox {
    public WAccountField(String text, double width) {
        super(text, width);
    }

    @Override
    protected boolean addChar(char c) {
        if(c != ' ') {
            return super.addChar(c);
        }
        return false;
    }
}