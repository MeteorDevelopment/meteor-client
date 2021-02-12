/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.utils.misc.Names;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;

import java.util.List;

public class WItemWithLabel extends WTable {
    private ItemStack itemStack;
    private final WItem item;
    private final WLabel label;

    public WItemWithLabel(ItemStack itemStack, String name) {
        this.itemStack = itemStack;
        this.item = add(new WItem(itemStack)).getWidget();
        this.label = add(new WLabel(name + getStringToAppend())).getWidget();
    }

    public WItemWithLabel(ItemStack itemStack) {
        this(itemStack, Names.get(itemStack.getItem()));
    }

    private String getStringToAppend() {
        String str = "";
        if (itemStack.getItem() == Items.POTION) {
            List<StatusEffectInstance> effects = PotionUtil.getPotion(itemStack).getEffects();
            if (effects.size() > 0) {
                str += " ";
                StatusEffectInstance effect = effects.get(0);
                if (effect.getAmplifier() > 0) str += effect.getAmplifier() + 1 + " ";
                str += "(" + StatusEffectUtil.durationToString(effect, 1) + ")";
            }
        }
        return str;
    }

    public void set(ItemStack itemStack) {
        this.itemStack = itemStack;
        item.itemStack = itemStack;
        label.setText(itemStack.getName().getString() + getStringToAppend());
    }

    public String getLabelText() {
        return label.getText();
    }
}
