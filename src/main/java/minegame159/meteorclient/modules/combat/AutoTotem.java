/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

//Updated by squidoodly 24/04/2020
//Updated by squidoodly 19/06/2020
//Updated by squidoodly 30/12/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.NoFall;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.DamageCalcUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.world.Dimension;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

@InvUtils.Priority(priority = Integer.MAX_VALUE)
public class AutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder()
            .name("smart")
            .description("Only switches to a totem when you are close to death.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> fallback = sgGeneral.add(new BoolSetting.Builder()
            .name("fallback")
            .description("Enables Offhand Extra when you run out of totems.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> inventorySwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("inventory")
            .description("Whether or not to equip totems while in your inventory.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
            .name("health")
            .description("The health Auto Totem's smart mode activates at.")
            .defaultValue(10)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private String totemCountString = "0";

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean locked = false;

    public AutoTotem() {
        super(Category.Combat, "auto-totem", "Automatically equips totems in your offhand.");
    }

    @Override
    public void onDeactivate() {
        locked = false;
    }

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        assert mc.player != null;
        if (mc.currentScreen instanceof InventoryScreen && !inventorySwitch.get()) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;

        InvUtils.FindItemResult result = InvUtils.findItemWithCount(Items.TOTEM_OF_UNDYING);

        if (result.count <= 0) {
            if (!ModuleManager.INSTANCE.get(OffhandExtra.class).isActive() && fallback.get()) {
                ModuleManager.INSTANCE.get(OffhandExtra.class).toggle();
            }

            ModuleManager.INSTANCE.get(OffhandExtra.class).setTotems(true);
        } else {
            ModuleManager.INSTANCE.get(OffhandExtra.class).setTotems(false);

            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING && (!smart.get()
                    || isLow())) {
                locked = true;
                moveTotem(result);
            } else if (smart.get() && !isLow()) {
                locked = false;
            }
        }

        totemCountString = Integer.toString(result.count);
    });

    @Override
    public String getInfoString() {
        return totemCountString;
    }

    private void moveTotem(InvUtils.FindItemResult result){
        assert mc.player != null;
        boolean empty = mc.player.getOffHandStack().isEmpty();
        List<Integer> slots = new ArrayList<>();
        if(mc.player.inventory.getCursorStack().getItem() != Items.TOTEM_OF_UNDYING) {
            slots.add(InvUtils.invIndexToSlotId(result.slot));
        }
        slots.add(InvUtils.invIndexToSlotId(InvUtils.OFFHAND_SLOT));
        if (!empty) slots.add(InvUtils.invIndexToSlotId(result.slot));
        InvUtils.addSlots(slots, this.getClass());
    }

    private double getHealthReduction(){
        assert mc.world != null;
        assert mc.player != null;
        double damageTaken = 0;
        for(Entity entity : mc.world.getEntities()){
            if(entity instanceof EndCrystalEntity && damageTaken < DamageCalcUtils.crystalDamage(mc.player, entity.getPos())){
                damageTaken = DamageCalcUtils.crystalDamage(mc.player, entity.getPos());
            }else if(entity instanceof PlayerEntity && damageTaken < DamageCalcUtils.getSwordDamage((PlayerEntity) entity, true)){
                if(FriendManager.INSTANCE.notTrusted((PlayerEntity) entity) && mc.player.getPos().distanceTo(entity.getPos()) < 5){
                    if(((PlayerEntity) entity).getActiveItem().getItem() instanceof SwordItem){
                        damageTaken = DamageCalcUtils.getSwordDamage((PlayerEntity) entity, true);
                    }
                }
            }
        }
        if(!ModuleManager.INSTANCE.get(NoFall.class).isActive() && mc.player.fallDistance > 3){
            double damage =mc.player.fallDistance * 0.5;
            if(damage > damageTaken){
                damageTaken = damage;
            }
        }
        if (Utils.getDimension() != Dimension.Nether) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                if (blockEntity instanceof BedBlockEntity && damageTaken < DamageCalcUtils.bedDamage(mc.player, new Vec3d(blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ()))) {
                    damageTaken = DamageCalcUtils.bedDamage(mc.player, new Vec3d(blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ()));
                }
            }
        }
        return damageTaken;
    }

    private double getHealth(){
        assert mc.player != null;
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    public boolean getLocked(){
        return locked;
    }

    private boolean isLow(){
        return getHealth() < health.get() || (getHealth() - getHealthReduction()) < health.get();
    }

}
