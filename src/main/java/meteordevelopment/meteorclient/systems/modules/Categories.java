/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.item.Items;

public class Categories {
    public static final Category Combat = new Category("Combat", Items.GOLDEN_SWORD.getDefaultStack(), Color.YELLOW);
    public static final Category Player = new Category("Player", Items.ARMOR_STAND.getDefaultStack(), Color.MAGENTA);
    public static final Category Movement = new Category("Movement", Items.DIAMOND_BOOTS.getDefaultStack(), Color.CYAN);
    public static final Category Render = new Category("Render", Items.GLASS.getDefaultStack(), Color.RED);
    public static final Category World = new Category("World", Items.GRASS_BLOCK.getDefaultStack(), Color.GREEN);
    public static final Category Misc = new Category("Misc", Items.LAVA_BUCKET.getDefaultStack(), Color.ORANGE);

    public static boolean REGISTERING;

    public static void init() {
        REGISTERING = true;

        // Meteor
        Modules.registerCategory(Combat);
        Modules.registerCategory(Player);
        Modules.registerCategory(Movement);
        Modules.registerCategory(Render);
        Modules.registerCategory(World);
        Modules.registerCategory(Misc);

        // Addons
        AddonManager.ADDONS.forEach(MeteorAddon::onRegisterCategories);

        REGISTERING = false;
    }
}
