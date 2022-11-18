/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.Text;

public class AutoLog extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("Automatically disconnects when health is lower or equal to this value.")
            .defaultValue(6)
            .range(0, 20)
            .sliderMax(20)
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

    private final Setting<Boolean> crystalLog = sgGeneral.add(new BoolSetting.Builder()
            .name("crystal-nearby")
            .description("Disconnects when a crystal appears near you.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("How close a crystal has to be to you before you disconnect.")
            .defaultValue(4)
            .range(1, 10)
            .sliderMax(5)
            .visible(crystalLog::get)
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
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLog] Health was lower than " + health.get() + ".")));
            if(smartToggle.get()) {
                this.toggle();
                enableHealthListener();
            }
        }

        if(smart.get() && playerHealth + mc.player.getAbsorptionAmount() - PlayerUtils.possibleHealthReductions() < health.get()){
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLog] Health was going to be lower than " + health.get() + ".")));
            if (toggleOff.get()) this.toggle();
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity.getUuid() != mc.player.getUuid()) {
                if (onlyTrusted.get() && entity != mc.player && !Friends.get().isFriend((PlayerEntity) entity)) {
                        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLog] A non-trusted player appeared in your render distance.")));
                        if (toggleOff.get()) this.toggle();
                        break;
                }
                if (PlayerUtils.isWithin(entity, 8) && instantDeath.get() && DamageUtils.getSwordDamage((PlayerEntity) entity, true)
                        > playerHealth + mc.player.getAbsorptionAmount()) {
                    mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLog] Anti-32k measures.")));
                    if (toggleOff.get()) this.toggle();
                    break;
                }
            }
            if (entity instanceof EndCrystalEntity && PlayerUtils.isWithin(entity, range.get()) && crystalLog.get()) {
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLog] End Crystal appeared within specified range.")));
                if (toggleOff.get()) this.toggle();
            }
        }
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

    private void enableHealthListener(){
        MeteorClient.EVENT_BUS.subscribe(staticListener);
    }
    private void disableHealthListener(){
        MeteorClient.EVENT_BUS.unsubscribe(staticListener);
    }
}
