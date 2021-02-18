/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules;

public class Categories {
    public static final Category Combat = new Category("Combat");
    public static final Category Player = new Category("Player");
    public static final Category Movement = new Category("Movement");
    public static final Category Render = new Category("Render");
    public static final Category Misc = new Category("Misc");

    public static void register() {
        Modules.registerCategory(Combat);
        Modules.registerCategory(Player);
        Modules.registerCategory(Movement);
        Modules.registerCategory(Render);
        Modules.registerCategory(Misc);
    }
}
