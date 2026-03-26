/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import net.minecraft.world.item.Items;

public class Categories {
    public static final Category Combat = new Category("Combat");
    public static final Category Player = new Category("Player");
    public static final Category Movement = new Category("Movement");
    public static final Category Render = new Category("Render");
    public static final Category World = new Category("World");
    public static final Category Misc = new Category("Misc");

    public static boolean REGISTERING;

    public static void init() {
        REGISTERING = true;

        Combat.setIcon(Items.GOLDEN_SWORD);
        Player.setIcon(Items.ARMOR_STAND);
        Movement.setIcon(Items.DIAMOND_BOOTS);
        Render.setIcon(Items.GLASS);
        World.setIcon(Items.GRASS_BLOCK);
        Misc.setIcon(Items.LAVA_BUCKET);

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
