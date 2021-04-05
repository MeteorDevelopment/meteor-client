/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.game;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class GetTooltipEvent {

    public ItemStack itemStack;
    public List<Text> list;

    public static class Modify extends GetTooltipEvent {
        private static final Modify INSTANCE = new Modify();

        public MatrixStack matrixStack;
        public int x, y;

        public static Modify get(ItemStack itemStack, List<Text> list, MatrixStack matrixStack, int x, int y) {
            INSTANCE.itemStack = itemStack;
            INSTANCE.list = list;
            INSTANCE.matrixStack = matrixStack;
            INSTANCE.x = x;
            INSTANCE.y = y;
            return INSTANCE;
        }
    }

    public static class Append extends GetTooltipEvent {
        private static final Append INSTANCE = new Append();

        public static Append get(ItemStack itemStack, List<Text> list) {
            INSTANCE.itemStack = itemStack;
            INSTANCE.list = list;
            return INSTANCE;
        }
    }

}
