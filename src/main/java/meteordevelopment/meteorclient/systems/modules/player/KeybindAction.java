/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

// Created by squidoodly 06/07/2020
// Modified by RickyTheRacc 12/22/2022
// Miss you Squid

import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class KeybindAction extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("keybind")
        .description("Which key should trigger the action.")
        .defaultValue(Keybind.fromKey(GLFW_MOUSE_BUTTON_MIDDLE))
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Which item to use when the keybind is clicked.")
            .defaultValue(Mode.Pearl)
            .build()
    );

    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder()
        .name("message")
        .description("Whether to message players when you add them as a friend.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Friend)
        .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-info")
        .description("Notifies you when you do not have the specified item in your hotbar.")
        .defaultValue(true)
        .build()
    );

    private boolean isUsing, wasHotbar;
    private int prevSlot;

    public KeybindAction() {
        super(Categories.Player, "keybind-action", "Lets you use items when you press a button.");
    }

    @Override
    public void onDeactivate() {
        stopIfUsing();
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || mc.currentScreen != null) return;
        if (keybind.get().getValue() != event.button || !keybind.get().isPressed()) return;

        doAction();
    }

    @EventHandler
    private void onKeyBoardButton(KeyEvent event) {
        if (event.action != KeyAction.Press || mc.currentScreen != null) return;
        if (keybind.get().getValue() != event.key || !keybind.get().isPressed()) return;

        doAction();
    }

    private void doAction() {
        if (mode.get() == Mode.Friend) {
            if (mc.targetedEntity instanceof PlayerEntity player) {

                if (!Friends.get().isFriend(player)) {
                    Friends.get().add(new Friend(player));
                    if (message.get()) {
                        String text = "/msg " + player.getEntityName() +  " I just added you as a friend.";
                        ChatUtils.sendPlayerMsg(text);
                    }
                } else Friends.get().remove(Friends.get().get(player));
            }

            return;
        }

        FindItemResult result = InvUtils.find(mode.get().item);
        if (!result.found()) {
            if (chatInfo.get()) warning("Unable to find specified item.");
            return;
        }

        wasHotbar = result.isHotbar();
        if (wasHotbar) InvUtils.swap(result.slot(), true);
        else {
            prevSlot = result.slot();
            InvUtils.move().from(result.slot()).to(mc.player.getInventory().selectedSlot);
        }


        if (mode.get().type == Type.Short) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            swapBack();
        } else {
            mc.options.useKey.setPressed(true);
            isUsing = true;
        }
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (isUsing) {
            boolean pressed = true;

            if (mc.player.getMainHandStack().getItem() instanceof BowItem) {
                pressed = BowItem.getPullProgress(mc.player.getItemUseTime()) < 1;
            }

            mc.options.useKey.setPressed(pressed);
        }
    }

    @EventHandler
    private void onFinishUsingItem(FinishUsingItemEvent event) {
        stopIfUsing();
    }

    @EventHandler
    private void onStoppedUsingItem(StoppedUsingItemEvent event) {
        stopIfUsing();
    }

    private void stopIfUsing() {
        if (isUsing) {
            mc.options.useKey.setPressed(false);
            swapBack();
            isUsing = false;
        }
    }

    private void swapBack() {
        if (wasHotbar) InvUtils.swapBack();
        else InvUtils.move().from(prevSlot).to(mc.player.getInventory().selectedSlot);
    }

    private enum Type {
        Short,
        Long
    }

    public enum Mode {
        Pearl (Items.ENDER_PEARL, Type.Short),
        Rocket (Items.FIREWORK_ROCKET, Type.Short),
        Gap (Items.GOLDEN_APPLE, Type.Long),
        EGap (Items.ENCHANTED_GOLDEN_APPLE, Type.Long),
        Chorus (Items.CHORUS_FRUIT, Type.Long),
        XP (Items.EXPERIENCE_BOTTLE, Type.Short),
        Friend (null, null);

        private final Item item;
        private final Type type;

        Mode(Item item, Type type) {
            this.item = item;
            this.type = type;
        }
    }
}
