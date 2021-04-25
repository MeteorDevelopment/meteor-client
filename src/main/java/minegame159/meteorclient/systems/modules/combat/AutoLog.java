/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.movement.NoFall;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.player.DamageCalcUtils;
import minegame159.meteorclient.utils.world.Dimension;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AutoLog extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("Automatically disconnects when health is lower or equal to this value.")
            .defaultValue(6)
            .min(0)
            .max(20)
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
            .min(1)
            .max(10)
            .sliderMax(5)
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
        if (mc.player.getHealth() <= 0) {
            this.toggle();
            return;
        }
        if (mc.player.getHealth() <= health.get()) {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("[AutoLog] Health was lower than " + health.get() + ".")));
            if(smartToggle.get()) {
                this.toggle();
                enableHealthListener();
            }
        }

        if(smart.get() && mc.player.getHealth() + mc.player.getAbsorptionAmount() - getHealthReduction() < health.get()){
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("[AutoLog] Health was going to be lower than " + health.get() + ".")));
            if (toggleOff.get()) this.toggle();
        }

        for (Entity entity : mc.world.getEntities()) {
            if(entity instanceof PlayerEntity && entity.getUuid() != mc.player.getUuid()) {
                if (onlyTrusted.get() && entity != mc.player && Friends.get().notTrusted((PlayerEntity) entity)) {
                        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("[AutoLog] A non-trusted player appeared in your render distance.")));
                        if (toggleOff.get()) this.toggle();
                        break;
                }
                if (mc.player.distanceTo(entity) < 8 && instantDeath.get() && DamageCalcUtils.getSwordDamage((PlayerEntity) entity, true)
                        > mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                    mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("[AutoLog] Anti-32k measures.")));
                    if (toggleOff.get()) this.toggle();
                    break;
                }
            }
            if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) < range.get() && crystalLog.get()) {
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("[AutoLog] End Crystal appeared within specified range.")));
                if (toggleOff.get()) this.toggle();
            }
        }
    }

    private double getHealthReduction() {
        double damageTaken = 0;

        for (Entity entity : mc.world.getEntities()) {
            // Check for end crystals
            if (entity instanceof EndCrystalEntity && damageTaken < DamageCalcUtils.crystalDamage(mc.player, entity.getPos())) {
                damageTaken = DamageCalcUtils.crystalDamage(mc.player, entity.getPos());
            }
            // Check for players holding swords
            else if (entity instanceof PlayerEntity && damageTaken < DamageCalcUtils.getSwordDamage((PlayerEntity) entity, true)) {
                if (Friends.get().notTrusted((PlayerEntity) entity) && mc.player.getPos().distanceTo(entity.getPos()) < 5) {
                    if (((PlayerEntity) entity).getActiveItem().getItem() instanceof SwordItem) {
                        damageTaken = DamageCalcUtils.getSwordDamage((PlayerEntity) entity, true);
                    }
                }
            }
        }

        // Check for fall distance with water check
        if (!Modules.get().isActive(NoFall.class) && mc.player.fallDistance > 3) {
            double damage = mc.player.fallDistance * 0.5;

            if (damage > damageTaken && !EntityUtils.isAboveWater(mc.player)) {
                damageTaken = damage;
            }
        }

        // Check for beds if in nether
        if (Utils.getDimension() != Dimension.Overworld) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                BlockPos bp = blockEntity.getPos();
                Vec3d pos = new Vec3d(bp.getX(), bp.getY(), bp.getZ());

                if (blockEntity instanceof BedBlockEntity && damageTaken < DamageCalcUtils.bedDamage(mc.player, pos)) {
                    damageTaken = DamageCalcUtils.bedDamage(mc.player, pos);
                }
            }
        }

        return damageTaken;
    }

    private class StaticListener {
        @EventHandler
        private void healthListener(TickEvent.Post event) {
            if (isActive()) disableHealthListener();

            else if (Utils.canUpdate()
                    && !mc.player.isDead()
                    && mc.player.getHealth() >= health.get()) {
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
