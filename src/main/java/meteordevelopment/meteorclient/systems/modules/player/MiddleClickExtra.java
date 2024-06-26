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

    private final Setting<Boolean> quickSwap = sgGeneral.add(new BoolSetting.Builder()
        .name("quick-swap")
        .description("Allows you to use items in your inventory by simulating hotbar key presses. May get flagged by anticheats.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.AddFriend)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Swap back to your original slot when you finish using an item.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.AddFriend && !quickSwap.get())
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
    private boolean wasHeld;
    private int itemSlot;
    private int selectedSlot;

    @Override
    public void onDeactivate() {
        stopIfUsing(false);
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || event.button != GLFW_MOUSE_BUTTON_MIDDLE || mc.currentScreen != null) return;

        if (mode.get() == Mode.AddFriend) {
            if (mc.targetedEntity == null) return;
            if (!(mc.targetedEntity instanceof PlayerEntity player)) return;

            if (!Friends.get().isFriend(player)) {
                Friends.get().add(new Friend(player));
                info("Added %s to friends", player.getName().getString());
                if (message.get()) ChatUtils.sendPlayerMsg("/msg " + player.getName() + " I just friended you on Meteor.");
            } else {
                Friends.get().remove(Friends.get().get(player));
                info("Removed %s from friends", player.getName().getString());
            }

            return;
        }

        FindItemResult result = InvUtils.find(mode.get().item);
        if (!result.found() || !result.isHotbar() && !quickSwap.get()) {
            if (notify.get()) warning("Unable to find specified item.");
            return;
        }

        selectedSlot = mc.player.getInventory().selectedSlot;
        itemSlot = result.slot();
        wasHeld = result.isMainHand();

        if (!wasHeld) {
            if (!quickSwap.get()) InvUtils.swap(result.slot(), swapBack.get());
            else InvUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
        }

        if (mode.get().immediate) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            swapBack(false);
        } else {
            mc.options.useKey.setPressed(true);
            isUsing = true;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isUsing) return;
        boolean pressed = true;

        if (mc.player.getMainHandStack().getItem() instanceof BowItem) {
            pressed = BowItem.getPullProgress(mc.player.getItemUseTime()) < 1;
        }

        mc.options.useKey.setPressed(pressed);
    }

    @EventHandler
    private void onPacketSendEvent(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            stopIfUsing(true);
        }
    }

    @EventHandler
    private void onStoppedUsingItem(StoppedUsingItemEvent event) {
        stopIfUsing(false);
    }

    @EventHandler
    private void onFinishUsingItem(FinishUsingItemEvent event) {
        stopIfUsing(false);
    }

    private void stopIfUsing(boolean wasCancelled) {
        if (isUsing) {
            swapBack(wasCancelled);
            mc.options.useKey.setPressed(false);
            isUsing = false;
        }
    }

    void swapBack(boolean wasCancelled) {
        if (wasHeld) return;

        if (quickSwap.get()) {
            InvUtils.quickSwap().fromId(selectedSlot).to(itemSlot);
        } else {
            if (!swapBack.get() || wasCancelled) return;
            InvUtils.swapBack();
        }
    }

    public enum Mode {
        Pearl(Items.ENDER_PEARL, true),
        XP(Items.EXPERIENCE_BOTTLE, true),
        Rocket(Items.FIREWORK_ROCKET, true),

        Bow(Items.BOW, false),
        Gap(Items.GOLDEN_APPLE, false),
        EGap(Items.ENCHANTED_GOLDEN_APPLE, false),
        Chorus(Items.CHORUS_FRUIT, false),

        AddFriend(null, true);

        private final Item item;
        private final boolean immediate;

        Mode(Item item, boolean immediate) {
            this.item = item;
            this.immediate = immediate;
        }
    }
}
