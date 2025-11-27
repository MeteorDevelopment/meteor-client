/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.game;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class ItemStackTooltipEvent {
    private final ItemStack itemStack;
    private List<Text> list;

    public ItemStackTooltipEvent(ItemStack itemStack, List<Text> list) {
        this.itemStack = itemStack;
        this.list = list;
    }

    public List<Text> list() {
        return list;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    public void appendStart(Text text) {
        copyIfImmutable();
        int index = list.isEmpty() ? 0 : 1;
        list.add(index, text);
    }

    public void appendEnd(Text text) {
        copyIfImmutable();
        list.add(text);
    }

    public void append(int index, Text text) {
        copyIfImmutable();
        list.add(index, text);
    }

    public void set(int index, Text text) {
        copyIfImmutable();
        list.set(index, text);
    }

    private void copyIfImmutable() {
        // ItemStack#getTooltip can sometimes return List.of(), which is immutable.
        // Some modules like BetterTooltips try to modify that list anyway, which causes a crash if we don't replace it.
        if (List.of().getClass().getSuperclass().isInstance(list)) {
            list = new ObjectArrayList<>(list);
        }
    }
}
