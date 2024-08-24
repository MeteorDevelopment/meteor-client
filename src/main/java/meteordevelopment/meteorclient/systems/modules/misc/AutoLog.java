/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.HashMap;
import java.util.Set;
import java.util.Map;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class AutoLog extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEntities = settings.createGroup("Entities");

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("Automatically disconnects when health is lower or equal to this value.")
        .defaultValue(6)
        .range(0, 19)
        .sliderMax(19)
        .build()
    );

    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder()
        .name("smart")
        .description("Disconnects when you're about to take enough damage to kill you.")
        .defaultValue(true)
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

    //Declaring variables outside the loop for better efficiency
    private final Object2IntMap<EntityType<?>> entityCounts = new Object2IntOpenHashMap<>();
    private int totalEntities = 0;

    public AutoLog() {
        super(Categories.Combat, "auto-log", "Automatically disconnects you when certain requirements are met.");
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
            if(smartToggle.get()) {
                this.toggle();
                enableHealthListener();
            }
        }

        if (smart.get() && playerHealth + mc.player.getAbsorptionAmount()
            - PlayerUtils.possibleHealthReductions() < health.get()) {
            disconnect("Health was going to be lower than " + health.get() + ".");
            if (toggleOff.get()) this.toggle();
        }

        if (!onlyTrusted.get() && !instantDeath.get() && entities.get().isEmpty())
            return; // only check all entities if needed

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player && player.getUuid() != mc.player.getUuid()) {
                if (onlyTrusted.get() && player != mc.player && !Friends.get().isFriend(player)) {
                    disconnect("A non-trusted player appeared in your render distance.");
                    if (toggleOff.get()) this.toggle();
                    break;
                }
                if (instantDeath.get() && PlayerUtils.isWithin(entity, 8) && DamageUtils.getAttackDamage(player, mc.player)
                    > playerHealth + mc.player.getAbsorptionAmount()) {
                    disconnect("Anti-32k measures.");
                    if (toggleOff.get())
                        this.toggle();
                    break;
                }
            }
        }

        // Entities detection Logic
        if (!entities.get().isEmpty()) {
            // Reset totalEntities count and clear the entityCounts map
            totalEntities = 0;
            entityCounts.clear();

            // Iterate through all entities in the world and count the ones that match the selected types and are within range
            for (Entity entity : mc.world.getEntities()) {
                if (PlayerUtils.isWithin(entity, range.get()) && entities.get().contains(entity.getType())) {
                    totalEntities++;
                    if (!useTotalCount.get()) {
                        entityCounts.put(entity.getType(), entityCounts.getOrDefault(entity.getType(), 0) + 1);
                    }
                }
            }

            if (useTotalCount.get() && totalEntities >= combinedEntityThreshold.get()) {
                disconnect("Total number of selected entities within range exceeded the limit.");
                if (toggleOff.get())
                    this.toggle();
            } else if (!useTotalCount.get()) {
                // Check if the count of each entity type exceeds the specified limit
                for (Object2IntMap.Entry<EntityType<?>> entry : entityCounts.object2IntEntrySet()) {
                    if (entry.getIntValue() >= individualEntityThreshold.get()) {
                        disconnect("Number of " + entry.getKey().getName().getString()
                            + " within range exceeded the limit.");
                        if (toggleOff.get())
                            this.toggle();
                        break;
                    }
                }
            }
        }
    }

    private void disconnect(String reason) {
        MutableText text = Text.literal("[AutoLog] " + reason);
        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);

        if (autoReconnect.isActive()) {
            text.append(Text.literal("\n\nINFO - AutoReconnect was disabled").withColor(Colors.GRAY));
            autoReconnect.toggle();
        }

        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(text));
    }

    private class StaticListener {
        @EventHandler
        private void healthListener(TickEvent.Post event) {
            if (isActive()) disableHealthListener();

            else if (Utils.canUpdate()
                && !mc.player.isDead()
                && mc.player.getHealth() > health.get()) {
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
