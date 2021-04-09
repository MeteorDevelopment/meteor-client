/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.DropItemsEvent;
import minegame159.meteorclient.events.entity.player.BreakBlockEvent;
import minegame159.meteorclient.events.entity.player.PickItemsEvent;
import minegame159.meteorclient.events.entity.player.PlaceBlockEvent;
import minegame159.meteorclient.events.game.OpenScreenEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;

public class Announcer extends Module {
    private static final double TICK = 1.0 / 20.0;

    private final Feature[] features = {
            new Moving(),
            new Mining(),
            new Placing(),
            new DropItems(),
            new PickItems(),
            new OpenContainer()
    };

    public Announcer() {
        super(Categories.Misc, "announcer", "Announces specified actions into chat.");
    }

    @Override
    public void onActivate() {
        for (Feature feature : features) {
            if (feature.isEnabled()) {
                MeteorClient.EVENT_BUS.subscribe(feature);
                feature.reset();
            }
        }
    }

    @Override
    public void onDeactivate() {
        for (Feature feature : features) {
            if (feature.isEnabled()) {
                MeteorClient.EVENT_BUS.unsubscribe(feature);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (Feature feature : features) {
            if (feature.isEnabled()) feature.tick();
        }
    }

    private abstract class Feature {
        protected SettingGroup sg;

        private final Setting<Boolean> enabled;

        protected Feature(String name, String enabledName, String enabledDescription) {
            this.sg = settings.createGroup(name);

            enabled = sg.add(new BoolSetting.Builder()
                    .name(enabledName)
                    .description(enabledDescription)
                    .defaultValue(true)
                    .onChanged(aBoolean -> {
                        if (isActive() && isEnabled()) {
                            MeteorClient.EVENT_BUS.subscribe(this);
                            reset();
                        } else if (isActive() && !isEnabled()) {
                            MeteorClient.EVENT_BUS.unsubscribe(this);
                        }
                    })
                    .build()
            );
        }

        abstract void reset();

        abstract void tick();

        boolean isEnabled() {
            return enabled.get();
        }
    }

    private class Moving extends Feature {
        private final Setting<String> msg = sg.add(new StringSetting.Builder()
                .name("moving-msg")
                .description("The chat message for moving a certain amount of blocks.")
                .defaultValue("I just moved {dist} blocks!")
                .build()
        );

        private final Setting<Double> delay = sg.add(new DoubleSetting.Builder()
                .name("moving-delay")
                .description("The amount of delay between moving messages in seconds.")
                .defaultValue(10)
                .sliderMax(60)
                .build()
        );

        private final Setting<Double> minDist = sg.add(new DoubleSetting.Builder()
                .name("moving-min-dist")
                .description("The minimum distance for a moving message to send into chat.")
                .defaultValue(10)
                .sliderMax(100)
                .build()
        );

        private double dist, timer;
        private double lastX, lastZ;
        private boolean first;

        Moving() {
            super("Moving", "moving-enabled", "Send msg how much you moved.");
        }

        @Override
        void reset() {
            dist = 0;
            timer = 0;
            first = true;
        }

        @Override
        void tick() {
            if (first) {
                first = false;
                updateLastPos();
            }

            double deltaX = mc.player.getX() - lastX;
            double deltaZ = mc.player.getZ() - lastZ;
            dist += Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            if (timer >= delay.get()) {
                timer = 0;

                if (dist >= minDist.get()) {
                    sendMsg();
                    dist = 0;
                }
            } else {
                timer += TICK;
            }

            updateLastPos();
        }

        void updateLastPos() {
            lastX = mc.player.getX();
            lastZ = mc.player.getZ();
        }

        void sendMsg() {
            mc.player.sendChatMessage(msg.get().replace("{dist}", String.format("%.1f", dist)));
        }
    }

    private class Mining extends Feature {
        private final Setting<String> msg = sg.add(new StringSetting.Builder()
                .name("mining-msg")
                .description("The chat message for mining blocks.")
                .defaultValue("I just mined {count} {block}!")
                .build()
        );

        private Block lastBlock;
        private int count;
        private double notBrokenTimer;

        Mining() {
            super("Mining", "mining-enabled", "Send msg how much blocks you mined.");
        }

        @Override
        void reset() {
            lastBlock = null;
            count = 0;
            notBrokenTimer = 0;
        }

        @EventHandler
        private void onBreakBlock(BreakBlockEvent event) {
            Block block = event.getBlockState(mc.world).getBlock();

            if (lastBlock != null && lastBlock != block) {
                sendMsg();
            }

            lastBlock = block;
            count++;
            notBrokenTimer = 0;
        }

        @Override
        void tick() {
            if (notBrokenTimer >= 2) {
                sendMsg();
            } else {
                notBrokenTimer += TICK;
            }
        }

        void sendMsg() {
            if (count > 0) {
                mc.player.sendChatMessage(msg.get().replace("{count}", Integer.toString(count)).replace("{block}", lastBlock.getName().getString()));
                count = 0;
            }
        }
    }

    private class Placing extends Feature {
        private final Setting<String> msg = sg.add(new StringSetting.Builder()
                .name("placing-msg")
                .description("The chat message for placing blocks.")
                .defaultValue("I just placed {count} {block}!")
                .build()
        );

        private Block lastBlock;
        private int count;
        private double notPlacedTimer;

        Placing() {
            super("Placing", "placing-enabled", "Send msg how much blocks you placed.");
        }

        @Override
        void reset() {
            lastBlock = null;
            count = 0;
            notPlacedTimer = 0;
        }

        @EventHandler
        private void onPlaceBlock(PlaceBlockEvent event) {
            if (lastBlock != null && lastBlock != event.block) {
                sendMsg();
            }

            lastBlock = event.block;
            count++;
            notPlacedTimer = 0;
        }

        @Override
        void tick() {
            if (notPlacedTimer >= 2) {
                sendMsg();
            } else {
                notPlacedTimer += TICK;
            }
        }

        void sendMsg() {
            if (count > 0) {
                mc.player.sendChatMessage(msg.get().replace("{count}", Integer.toString(count)).replace("{block}", lastBlock.getName().getString()));
                count = 0;
            }
        }
    }

    private class DropItems extends Feature {
        private final Setting<String> msg = sg.add(new StringSetting.Builder()
                .name("drop-items-msg")
                .description("The chat message for dropping items.")
                .defaultValue("I just dropped {count} {item}!")
                .build()
        );

        private Item lastItem;
        private int count;
        private double notDroppedTimer;

        DropItems() {
            super("Drop Items", "drop-items-enabled", "Send msg how much items you dropped.");
        }

        @Override
        void reset() {
            lastItem = null;
            count = 0;
            notDroppedTimer = 0;
        }

        @EventHandler
        private void onDropItems(DropItemsEvent event) {
            if (lastItem != null && lastItem != event.itemStack.getItem()) {
                sendMsg();
            }

            lastItem = event.itemStack.getItem();
            count += event.itemStack.getCount();
            notDroppedTimer = 0;
        }

        @Override
        void tick() {
            if (notDroppedTimer >= 1) {
                sendMsg();
            } else {
                notDroppedTimer += TICK;
            }
        }

        void sendMsg() {
            if (count > 0) {
                mc.player.sendChatMessage(msg.get().replace("{count}", Integer.toString(count)).replace("{item}", lastItem.getName().getString()));
                count = 0;
            }
        }
    }

    private class PickItems extends Feature {
        private final Setting<String> msg = sg.add(new StringSetting.Builder()
                .name("pick-items-msg")
                .description("The chat message for picking up items.")
                .defaultValue("I just picked up {count} {item}!")
                .build()
        );

        private Item lastItem;
        private int count;
        private double notPickedUpTimer;

        PickItems() {
            super("Pick Items", "pick-items-enabled", "Send msg how much items you pick up.");
        }

        @Override
        void reset() {
            lastItem = null;
            count = 0;
            notPickedUpTimer = 0;
        }

        @EventHandler
        private void onPickItems(PickItemsEvent event) {
            if (lastItem != null && lastItem != event.itemStack.getItem()) {
                sendMsg();
            }

            lastItem = event.itemStack.getItem();
            count += event.itemStack.getCount();
            notPickedUpTimer = 0;
        }

        @Override
        void tick() {
            if (notPickedUpTimer >= 1) {
                sendMsg();
            } else {
                notPickedUpTimer += TICK;
            }
        }

        void sendMsg() {
            if (count > 0) {
                mc.player.sendChatMessage(msg.get().replace("{count}", Integer.toString(count)).replace("{item}", lastItem.getName().getString()));
                count = 0;
            }
        }
    }

    private class OpenContainer extends Feature {
        private final Setting<String> msg = sg.add(new StringSetting.Builder()
                .name("open-container-msg")
                .description("The chat message for opening a container.")
                .defaultValue("I just opened {name}!")
                .build()
        );

        public OpenContainer() {
            super("Open Container", "open-container-enabled", "Sends msg when you open containers.");
        }

        @Override
        void reset() {}

        @Override
        void tick() {}

        @EventHandler
        private void onOpenScreen(OpenScreenEvent event) {
            if (event.screen instanceof HandledScreen<?>) {
                String name = event.screen.getTitle().getString();
                if (!name.isEmpty()) sendMsg(name);
            }
        }

        void sendMsg(String name) {
            mc.player.sendChatMessage(msg.get().replace("{name}", name));
        }
    }
}
