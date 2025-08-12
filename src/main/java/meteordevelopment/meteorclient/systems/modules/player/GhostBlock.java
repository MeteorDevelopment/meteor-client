/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;

import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_G;

/**
 * @author NDev007
 */
public class GhostBlock extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Block> block = sgGeneral.add(new BlockSetting.Builder()
        .name("block")
        .description("Which block will be placed as a ghost.")
        .defaultValue(Blocks.DIAMOND_BLOCK)
        .build()
    );

    public final Setting<Keybind> ghostPlace = sgGeneral.add(new KeybindSetting.Builder()
        .name("ghost-block-place")
        .description("Starts place ghost block when this button is pressed.")
        .defaultValue(Keybind.fromKey(GLFW_KEY_G))
        .build()
    );

    public GhostBlock() {
        super(Categories.Player, "ghost-block", "Makes the ghost block on the client side");
    }
}
