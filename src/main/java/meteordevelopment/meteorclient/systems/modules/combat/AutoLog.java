/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

public class AutoLog extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEntities = settings.createGroup("Entities");

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("Automatically disconnects when health is lower or equal to this value. Set to 0 to disable.")
        .defaultValue(6)
        .range(0, 19)
        .sliderMax(19)
        .build()
    );

    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder()
        .name("predict-incoming-damage")
        .description("Disconnects when it detects you're about to take enough damage to set you under the 'health' setting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> totemPops = sgGeneral.add(new IntSetting.Builder()
        .name("totem-pops")
        .description("Disconnects when you have popped this many totems. Set to 0 to disable.")
        .defaultValue(0)
        .min(0)
        .build()
    );

    private final Setting<Boolean> onlyTrusted = sgGeneral.add(new BoolSetting.Builder()
        .name("only-trusted")
        .description("Disconnects when a player not on your friends list appears in render distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> instantDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("32K")
        .description("Disconnects when a player near you can instantly kill you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> smartToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("smart-toggle")
        .description("Disables Auto Log after a low-health logout. WILL re-enable once you heal.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOff = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-off")
        .description("Disables Auto Log after usage.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> toggleAutoReconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-auto-reconnect")
        .description("Whether to disable Auto Reconnect after a logout.")
        .defaultValue(true)
        .build()
    );

    // Entities

    private final Setting<Set<EntityType<?>>> entities = sgEntities.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Disconnects when a specified entity is present within a specified range.")
        .defaultValue(EntityType.END_CRYSTAL)
        .build()
    );

    private final Setting<Boolean> useTotalCount = sgEntities.add(new BoolSetting.Builder()
        .name("use-total-count")
        .description("Toggle between counting the total number of all selected entities or each entity individually.")
        .defaultValue(true)
        .visible(() -> !entities.get().isEmpty())
        .build());

    private final Setting<Integer> combinedEntityThreshold = sgEntities.add(new IntSetting.Builder()
        .name("combined-entity-threshold")
        .description("The minimum total number of selected entities that must be near you before disconnection occurs.")
        .defaultValue(10)
        .min(1)
        .sliderMax(32)
        .visible(() -> useTotalCount.get() && !entities.get().isEmpty())
        .build()
    );

    private final Setting<Integer> individualEntityThreshold = sgEntities.add(new IntSetting.Builder()
        .name("individual-entity-threshold")
        .description("The minimum number of entities individually that must be near you before disconnection occurs.")
        .defaultValue(2)
        .min(1)
        .sliderMax(16)
        .visible(() -> !useTotalCount.get() && !entities.get().isEmpty())
        .build()
    );

    private final Setting<Integer> range = sgEntities.add(new IntSetting.Builder()
        .name("range")
        .description("How close an entity has to be to you before you disconnect.")
        .defaultValue(5)
        .min(1)
        .sliderMax(16)
        .visible(() -> !entities.get().isEmpty())
        .build()
    );

    // Declaring variables outside the loop for better efficiency
    private final Object2IntMap<EntityType<?>> entityCounts = new Object2IntOpenHashMap<>();

    private int pops;

    public AutoLog() {
        super(Categories.Combat, "auto-log", "Automatically disconnects you when certain requirements are met.");
    }

    @Override
    public void onActivate() {
        pops = 0;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundEntityEventPacket p)) return;
        if (p.getEventId() != EntityEvent.PROTECTED_FROM_DEATH) return;

        Entity entity = p.getEntity(mc.level);
        if (entity == null || !entity.equals(mc.player)) return;

        pops++;
        if (totemPops.get() > 0 && pops >= totemPops.get()) {
            disconnect("Popped " + pops + " totems.");
            if (toggleOff.get()) this.toggle();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        float playerHealth = mc.player.getHealth();
        if (playerHealth <= 0) {
            this.toggle();
            return;
        }
        if (playerHealth <= health.get()) {
            disconnect("Health was lower than " + health.get() + ".");
            if (smartToggle.get()) {
                if (isActive()) this.toggle();
                enableHealthListener();
            } else if (toggleOff.get()) this.toggle();
            return;
        }

        if (smart.get() && playerHealth + mc.player.getAbsorptionAmount() - PlayerUtils.possibleHealthReductions() < health.get()) {
            disconnect("Health was going to be lower than " + health.get() + ".");
            if (toggleOff.get()) this.toggle();
            return;
        }

        if (!onlyTrusted.get() && !instantDeath.get() && entities.get().isEmpty())
            return; // only check all entities if needed

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player player && player.getUUID() != mc.player.getUUID()) {
                if (onlyTrusted.get() && player != mc.player && !Friends.get().isFriend(player)) {
                    disconnect(Component.literal("Non-trusted player '" + ChatFormatting.RED + player.getName().getString() + ChatFormatting.WHITE + "' appeared in your render distance."));
                    if (toggleOff.get()) this.toggle();
                    return;
                }

                if (instantDeath.get() && PlayerUtils.isWithin(entity, 8) && DamageUtils.getAttackDamage(player, mc.player)
                    > playerHealth + mc.player.getAbsorptionAmount()) {
                    disconnect("Anti-32k measures.");
                    if (toggleOff.get()) this.toggle();
                    return;
                }
            }
        }

        // Entities detection Logic
        if (!entities.get().isEmpty()) {
            // Reset totalEntities count and clear the entityCounts map
            int totalEntities = 0;
            entityCounts.clear();

            // Iterate through all entities in the world and count the ones that match the selected types and are within range
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (PlayerUtils.isWithin(entity, range.get()) && entities.get().contains(entity.getType())) {
                    totalEntities++;
                    if (!useTotalCount.get()) {
                        entityCounts.put(entity.getType(), entityCounts.getOrDefault(entity.getType(), 0) + 1);
                    }
                }
            }

            if (useTotalCount.get() && totalEntities >= combinedEntityThreshold.get()) {
                disconnect("Total number of selected entities within range exceeded the limit.");
                if (toggleOff.get()) this.toggle();
            } else if (!useTotalCount.get()) {
                // Check if the count of each entity type exceeds the specified limit
                for (Object2IntMap.Entry<EntityType<?>> entry : entityCounts.object2IntEntrySet()) {
                    if (entry.getIntValue() >= individualEntityThreshold.get()) {
                        disconnect("Number of " + entry.getKey().getDescription().getString() + " within range exceeded the limit.");
                        if (toggleOff.get()) this.toggle();
                        return;
                    }
                }
            }
        }
    }

    private void disconnect(String reason) {
        disconnect(Component.literal(reason));
    }

    private void disconnect(Component reason) {
        MutableComponent text = Component.literal("[AutoLog] ");
        text.append(reason);

        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (autoReconnect.isActive() && toggleAutoReconnect.get()) {
            text.append(Component.literal("\n\nINFO - AutoReconnect was disabled").withColor(CommonColors.GRAY));
            autoReconnect.toggle();
        }

        mc.player.connection.handleDisconnect(new ClientboundDisconnectPacket(text));
    }

    private class StaticListener {
        @EventHandler
        private void healthListener(TickEvent.Post event) {
            if (isActive()) disableHealthListener();

            else if (Utils.canUpdate()
                && !mc.player.isDeadOrDying()
                && mc.player.getHealth() > health.get()) {
                info("Player health greater than minimum, re-enabling module.");
                toggle();
                disableHealthListener();
            }
        }
    }

    private final StaticListener staticListener = new StaticListener();

    private void enableHealthListener() {
        MeteorClient.EVENT_BUS.subscribe(staticListener);
    }

    private void disableHealthListener() {
        MeteorClient.EVENT_BUS.unsubscribe(staticListener);
    }
}
