/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixin.MinecraftClientAccessor;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;

public class FastUse extends Module {
    public enum Item {
        All,
        Exp,
        Crystal,
        ExpAndCrystal
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<CustomFastUse.Item> itemChoose = sgGeneral.add(new EnumSetting.Builder<CustomFastUse.Item>()
            .name("Which item")
            .description(".")
            .defaultValue(CustomFastUse.Item.All)
            .build()
    );

    public FastUse() {
        super(Categories.Player, "fast-use", "Allows you to use items at very high speeds.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
            switch(itemChoose.get()) {
            case All:
                setClickDelay();
                break;
            case Exp:
                assert mc.player != null;
                if(mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem || mc.player.getOffHandStack().getItem() instanceof ExperienceBottleItem)
                    setClickDelay();
                break;
            case Crystal:
                assert mc.player != null;
                if(mc.player.getMainHandStack().getItem() instanceof EndCrystalItem || mc.player.getOffHandStack().getItem() instanceof EndCrystalItem)
                    setClickDelay();
                break;
            case ExpAndCrystal:
                assert mc.player != null;
                if(mc.player.getMainHandStack().getItem() instanceof EndCrystalItem || mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem ||
                        mc.player.getOffHandStack().getItem() instanceof EndCrystalItem || mc.player.getOffHandStack().getItem() instanceof ExperienceBottleItem)
                    setClickDelay();
                break;
        }
    }
    
        private void setClickDelay() {
        ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
    }
}
