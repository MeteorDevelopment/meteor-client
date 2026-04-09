/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Iterator;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WItemWithLabel extends WHorizontalList {
    private ItemStack itemStack;
    private String name;

    private WItem item;
    private WLabel label;

    public WItemWithLabel(ItemStack itemStack, String name) {
        this.itemStack = itemStack;
        this.name = name;
    }

    @Override
    public void init() {
        item = add(theme.item(itemStack)).widget();
        label = add(theme.label(name + getStringToAppend())).widget();
    }

    private String getStringToAppend() {
        String str = "";

        if (itemStack.getItem() == Items.POTION) {
            Iterator<MobEffectInstance> effects = itemStack.getItem().components().get(DataComponents.POTION_CONTENTS).getAllEffects().iterator();
            if (!effects.hasNext()) return str;

            str += " ";

            MobEffectInstance effect = effects.next();
            if (effect.getAmplifier() > 0) str += "%d ".formatted(effect.getAmplifier() + 1);

            str += "(%s)".formatted(MobEffectUtil.formatDuration(effect, 1, mc.level != null ? mc.level.tickRateManager().tickrate() : 20.0F).getString());
        }

        return str;
    }

    public void set(ItemStack itemStack) {
        this.itemStack = itemStack;
        item.itemStack = itemStack;

        name = Names.get(itemStack);
        label.set(name + getStringToAppend());
    }

    public String getLabelText() {
        return label == null ? name : label.get();
    }
}
