/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.utils.Init;
import meteordevelopment.meteorclient.utils.InitStage;

public class Shaders {
    public static Shader POS_COLOR;
    public static Shader POS_TEX_COLOR;
    public static Shader TEXT;

    @Init(stage = InitStage.Pre)
    public static void init() {
        POS_COLOR = new Shader("pos_color.vert", "pos_color.frag");
        POS_TEX_COLOR = new Shader("pos_tex_color.vert", "pos_tex_color.frag");
        TEXT = new Shader("text.vert", "text.frag");
    }
}
