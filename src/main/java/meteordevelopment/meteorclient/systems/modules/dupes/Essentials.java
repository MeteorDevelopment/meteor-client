/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.dupes;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import java.io.File;
import java.io.IOException;

import java.awt.*;
import java.awt.event.KeyEvent;


public class Essentials extends Module {
    public Essentials() {
        super(Categories.Dupes, "Essentials Recipe", "Automates the essentials recipe dupe");
    }

    private final SettingGroup sgGeneral = settings.createGroup("Items");

    private final Setting<Boolean> gapple = sgGeneral.add(new BoolSetting.Builder()
        .name("Enchanted Golden Apple")
        .description("Dupes Gapples")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> wither_skull = sgGeneral.add(new BoolSetting.Builder()
        .name("Wither Skeleton Skull")
        .description("Dupes Wither Skulls")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> netherite = sgGeneral.add(new BoolSetting.Builder()
        .name("Netherite ")
        .description("Dupes Netherite")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> diamonds = sgGeneral.add(new BoolSetting.Builder()
        .name("Diamonds ")
        .description("Dupes Diamonds")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> creeper_head = sgGeneral.add(new BoolSetting.Builder()
        .name("Creeper Heads ")
        .description("Dupes Creeper Heads")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> iron = sgGeneral.add(new BoolSetting.Builder()
        .name("Iron ")
        .description("Dupes iron")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> emeralds = sgGeneral.add(new BoolSetting.Builder()
        .name("Emeralds ")
        .description("Dupes Emeralds")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> eyes = sgGeneral.add(new BoolSetting.Builder()
        .name("Eyes Of Ender ")
        .description("Dupes Eyes Of Ender")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pearls = sgGeneral.add(new BoolSetting.Builder()
        .name("Ender Pearls ")
        .description("Dupes Ender Pearls")
        .defaultValue(false)
        .build()
    );

    private final SettingGroup sgConfig = settings.createGroup("Command");

    private final Setting<Boolean> recipe = sgConfig.add(new BoolSetting.Builder()
        .name("/recipe")
        .description("uses /recipe")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> recipes = sgConfig.add(new BoolSetting.Builder()
        .name("/recipes")
        .description("uses /recipes")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> formula = sgConfig.add(new BoolSetting.Builder()
        .name("/forumula")
        .description("uses /formula")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> eformula = sgConfig.add(new BoolSetting.Builder()
        .name("/eforumula")
        .description("uses /eformula")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> method = sgConfig.add(new BoolSetting.Builder()
        .name("/method")
        .description("uses /method")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> emethod = sgConfig.add(new BoolSetting.Builder()
        .name("/emethod")
        .description("uses /emethod")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between specified messages in ticks.")
        .defaultValue(20)
        .min(20)
        .sliderMax(200)
        .build()
    );

    private final Setting<Integer> slot = sgGeneral.add(new IntSetting.Builder()
        .name("slot")
        .description("Slot to drop from")
        .defaultValue(1)
        .min(0)
        .sliderMax(9)
        .build()
    );

    private int messageI, timer;

    @Override
    public void onActivate() {
        timer = delay.get();
        messageI = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        String item = ("");
        int slot=(0);
        String command=null;
        String start=("");
        int x =(0);
        int y =(0);
        if (recipe.get()){
            start=("/recipe ");
        }
        if (recipes.get()){
            start=("/recipes ");
        }
        if (formula.get()){
            start=("/formula ");
        }
        if (eformula.get()){
            start=("/eformula ");
        }
        if (method.get()){
            start=("/method ");
        }
        if (emethod.get()){
            start=("/emethod ");
        }

        if (gapple.get()) {
            item = ("mojang_banner_pattern");
            command = (start + item);
            slot=(2);
            x=(869);
            y=(370);
        } else if (wither_skull.get()) {
            item = ("skull_banner_pattern");
            command = (start + item);
            slot=(2);
            x=(869);
            y=(370);
        } else if (netherite.get()) {
            item = ("netherite_ingot");
            slot=(1);
            x =(807);
            y =(364);
            command = (start + item);
        } else if (diamonds.get()) {
            item = ("diamond");
            slot=(1);
            x =(807);
            y =(364);
            command = (start + item + " " + slot);
        } else if (creeper_head.get()) {
            item = ("creeper_banner_pattern");
            command = (start + item);
            slot=(2);
            x=(869);
            y=(370);
        } else if (iron.get()) {
            item = ("iron_ingot");
            slot=(1);
            x =(807);
            y =(364);
            command = (start + item + " " + slot);
        } else if (emeralds.get()) {
            item = ("emerald");
            x =(807);
            y =(364);
            slot=(1);
            command = ("/recipe " + item + " " + slot);
        } else if (eyes.get()) {
            item = ("ender_eye");
            x =(1090);
            y =(420);
            command = ("/recipe " + item);
        } else if (pearls.get()) {
            item = ("ender_eye");
            x =(807);
            y =(364);
            slot=(1);
            command = ("/recipe " + item);
        }
        if (timer <= 0) {
            int i;
            i = messageI++;
            ChatUtils.sendPlayerMsg(command);
            //ChatUtils.sendPlayerMsg("dropped slot: "+i);
            try {
                Robot robot = new Robot();
                robot.mouseMove(x, y);
                robot.delay(100);
                InvUtils.drop().slotId(slot);
                //info("dropped slot " + i);
                //info("mouse moved to " +"x "+x+",y "+y);
                timer = delay.get();
            }catch (AWTException e) {
                return;
            }
        }
        else {
            timer--;
        }
    }
}
