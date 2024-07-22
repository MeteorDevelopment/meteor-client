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
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class AutoLog extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Select specific entities.")
        .defaultValue(EntityType.END_CRYSTAL)
        .build()
    );

    private final Setting<Boolean> TotalCount = sgGeneral.add(new BoolSetting.Builder()
        .name("Total the Entities")
        .description("Whether total number of all selected entites or each entity")
        .defaultValue(false)
        .visible(() -> !entities.get().isEmpty())
        .build());

    private final Setting<Integer> TotCount = sgGeneral.add(new IntSetting.Builder() //Number of TNT Minecart
        .name("Total Count")
        .description("Total number of all selected entites combined have to be near you before you disconnect.")
        .defaultValue(10)
        .range(1, 96)
        .sliderMax(32)
        .visible(() -> TotalCount.get() && !entities.get().isEmpty())
        .build()
    );

    private final Setting<Integer> EachCount = sgGeneral.add(new IntSetting.Builder() //Number of TNT Minecart
        .name("Each Count")
        .description("Minimum number of each entity have to be near you before you disconnect.")
        .defaultValue(2)
        .range(1, 96)
        .sliderMax(16)
        .visible(() -> !TotalCount.get() && !entities.get().isEmpty())
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder() //range for TNT Minecart
        .name("Range")
        .description("How close a entity has to be to you before you disconnect.")
        .defaultValue(5)
        .range(1, 128)
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

        if (smart.get() && playerHealth + mc.player.getAbsorptionAmount() - PlayerUtils.possibleHealthReductions() < health.get()){
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

        if (!entities.get().isEmpty()) {
            int totalEntities = 0;
            Map<EntityType<?>, Integer> entityCounts = new HashMap<>();

            for (Entity entity : mc.world.getEntities()) {
                if (PlayerUtils.isWithin(entity, range.get()) && entities.get().contains(entity.getType())) {
                    totalEntities++;
                    entityCounts.put(entity.getType(), entityCounts.getOrDefault(entity.getType(), 0) + 1);
                }
            }

            if (TotalCount.get() && totalEntities >= TotCount.get()) {
                disconnect("Total number of selected entities within range exceeded the limit.");
                if (toggleOff.get()) this.toggle();
            } else if (!TotalCount.get()) {
                for (Map.Entry<EntityType<?>, Integer> entry : entityCounts.entrySet()) {
                    if (entry.getValue() >= EachCount.get()) {
                        disconnect("Number of " + entry.getKey().getName().getString() + " within range exceeded the limit.");
                        if (toggleOff.get()) this.toggle();
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
