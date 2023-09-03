/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class MiddleClickExtra extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Which item to use when you middle click.")
        .defaultValue(Mode.Pearl)
        .build()
    );

    private final Setting<Boolean> message = sgGeneral.add(new BoolSetting.Builder()
        .name("message")
        .description("Sends a message to the player when you add them as a friend.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.AddFriend)
        .build()
    );

    private final Setting<SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
        .name("switch-mode")
        .description("How to swap to the item.")
        .defaultValue(SwitchMode.Silent)
        .visible(() -> mode.get() != Mode.AddFriend)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Notifies you when you do not have the specified item in your hotbar.")
        .defaultValue(true)
        .visible(() -> mode.get() != Mode.AddFriend)
        .build()
    );

    public MiddleClickExtra() {
        super(Categories.Player, "middle-click-extra", "Perform various actions when you middle click.");
    }

    private boolean isUsing;
    private int itemSlot;

    @Override
    public void onDeactivate() {
        stopIfUsing();
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || event.button != GLFW_MOUSE_BUTTON_MIDDLE || mc.currentScreen != null) return;

        if (mode.get() == Mode.AddFriend) {
            if (mc.targetedEntity == null) return;
            if (!(mc.targetedEntity instanceof PlayerEntity player)) return;

            if (!Friends.get().isFriend(player)) {
                Friends.get().add(new Friend(player));
                info("Added %s to friends", player.getEntityName());
                if (message.get()) ChatUtils.sendPlayerMsg("/msg " + player.getEntityName() + " I just friended you on Meteor.");
            } else {
                Friends.get().remove(Friends.get().get(player));
                info("Removed %s from friends", player.getEntityName());
            }

            return;
        }

        FindItemResult result = InvUtils.find(mode.get().item);
        if (!result.found() || !result.isHotbar() && switchMode.get() != SwitchMode.Inventory) {
            if (notify.get()) warning("Unable to find specified item.");
            return;
        }

        itemSlot = result.slot();

        if (result.isHotbar()) {
            InvUtils.swap(itemSlot, switchMode.get() != SwitchMode.Normal);
        } else {
            InvUtils.quickSwap().fromId(mc.player.getInventory().selectedSlot).toId(itemSlot);
        }

        switch (mode.get().type) {
            case Immediate -> {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                swapBack();
            }
            case LongerSingleClick -> mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            case Longer -> {
                mc.options.useKey.setPressed(true);
                isUsing = true;
            }
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
    private void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            stopIfUsing();
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

    void swapBack() {
        if (itemSlot >= 0 && itemSlot <= 8) {
            InvUtils.swapBack();
        } else {
            InvUtils.quickSwap().fromId(mc.player.getInventory().selectedSlot).toId(itemSlot);
        }
    }

    public enum Mode {
        Pearl(Items.ENDER_PEARL, Type.Immediate),
        Rocket(Items.FIREWORK_ROCKET, Type.Immediate),

        Rod(Items.FISHING_ROD, Type.LongerSingleClick),

        Bow(Items.BOW, Type.Longer),
        Gap(Items.GOLDEN_APPLE, Type.Longer),
        EGap(Items.ENCHANTED_GOLDEN_APPLE, Type.Longer),
        Chorus(Items.CHORUS_FRUIT, Type.Longer),
        XP(Items.EXPERIENCE_BOTTLE, Type.Immediate),

        AddFriend(null, null);

        private final Item item;
        private final Type type;

        Mode(Item item, Type type) {
            this.item = item;
            this.type = type;
        }
    }

    public enum SwitchMode {
        Normal,
        Silent,
        Inventory
    }

    private enum Type {
        Immediate,
        LongerSingleClick,
        Longer
    }
}
